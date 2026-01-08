package com.ershi.aspider.orchestration.service;

import com.ershi.aspider.datasource.service.FinancialArticleDSFactory;
import com.ershi.aspider.datasource.domain.FinancialArticle;
import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.datasource.provider.FinancialArticleDataSource;
import com.ershi.aspider.embedding.EmbeddingExecutor;
import com.ershi.aspider.processor.cleaner.FinancialArticleCleaner;
import com.ershi.aspider.processor.extractor.ContentExtractor;
import com.ershi.aspider.storage.elasticsearch.service.FinancialArticleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据处理编排服务，只负责编排，不做实际业务处理
 *
 * @author Ershi-Gu.
 * @since 2025-11-21
 */
@Service
public class FinancialArticleDataService {

    private static final Logger log = LoggerFactory.getLogger(FinancialArticleDataService.class);

    /** 默认查询批次大小 */
    private static final int DEFAULT_QUERY_BATCH_SIZE = 500;

    private final FinancialArticleDSFactory financialArticleDSFactory;
    private final FinancialArticleCleaner financialArticleCleaner;
    private final ContentExtractor contentExtractor;
    private final EmbeddingExecutor embeddingExecutor;
    private final FinancialArticleStorageService storageService;

    public FinancialArticleDataService(FinancialArticleDSFactory financialArticleDSFactory, FinancialArticleCleaner financialArticleCleaner,
        ContentExtractor contentExtractor, EmbeddingExecutor embeddingExecutor, FinancialArticleStorageService storageService) {
        this.financialArticleDSFactory = financialArticleDSFactory;
        this.financialArticleCleaner = financialArticleCleaner;
        this.contentExtractor = contentExtractor;
        this.embeddingExecutor = embeddingExecutor;
        this.storageService = storageService;
    }

    /**
     * 处理所有数据源的新闻数据
     */
    public int processAllFinancialArticle() {
        log.info("========== 开始处理所有数据源新闻数据 ==========");

        List<FinancialArticle> allFinancialArticle = fetchFromAllDataSources();
        if (allFinancialArticle.isEmpty()) {
            log.warn("未获取到任何新闻数据");
            return 0;
        }

        return executePipeline(allFinancialArticle);
    }

    /**
     * 处理指定数据源的新闻数据，用于定时抓取场景，不执行摘要提取和向量化
     */
    public int processFinancialArticleRawOnly(FinancialArticleDSTypeEnum sourceType) {
        log.info("========== 开始处理数据源 [{}] ==========", sourceType.getDesc());

        List<FinancialArticle> financialArticles = fetchFromDataSource(sourceType);
        if (financialArticles.isEmpty()) {
            log.warn("数据源 [{}] 未获取到数据", sourceType.getDesc());
            return 0;
        }

        return executePipeline(financialArticles);
    }

    /**
     * 原始数据处理管道
     */
    private int executePipeline(List<FinancialArticle> financialArticle) {
        // 数据清洗
        List<FinancialArticle> cleanedData = financialArticleCleaner.clean(financialArticle);
        if (cleanedData.isEmpty()) {
            log.warn("清洗后无有效数据");
            return 0;
        }

        // 标记为未向量化处理
        cleanedData.forEach(item -> item.setProcessed(false));

        // 持久化
        int successCount = storageService.batchSaveToEs(cleanedData);

        log.info("========== [阶段一] 完成，保存 {} 条原始数据 ==========", successCount);
        return successCount;
    }

    /**
     * 从所有数据源获取数据
     */
    private List<FinancialArticle> fetchFromAllDataSources() {
        List<FinancialArticleDataSource> dataSources = financialArticleDSFactory.getAllDataSources();
        log.info("发现 {} 个数据源", dataSources.size());

        List<FinancialArticle> allData = new ArrayList<>();

        for (FinancialArticleDataSource financialArticleDS : dataSources) {
            try {
                String sourceDesc = financialArticleDS.getDataSourceType().getDesc();
                log.info("正在获取数据源 [{}] 的数据...", sourceDesc);

                // 执行数据请求
                List<FinancialArticle> data = financialArticleDS.getFinancialArticle();
                log.info("数据源 [{}] 获取到 {} 条数据", sourceDesc, data.size());

                allData.addAll(data);

            } catch (Exception e) {
                log.error("数据源 [{}] 获取数据失败",
                          financialArticleDS.getDataSourceType().getType(), e);
            }
        }

        log.info("所有数据源共获取 {} 条数据", allData.size());
        return allData;
    }

    /**
     * 从指定数据源获取数据
     */
    private List<FinancialArticle> fetchFromDataSource(FinancialArticleDSTypeEnum sourceType) {
        FinancialArticleDataSource dataSource = financialArticleDSFactory.getDataSource(sourceType);
        return dataSource.getFinancialArticle();
    }

    /**
     * 向量化处理
     */
    private void embedData(List<FinancialArticle> financialArticle) {
        log.info("开始向量化 {} 条新闻数据", financialArticle.size());

        try {
            log.info("正在向量化标题...");
            List<String> titles = financialArticle.stream()
                .map(FinancialArticle::getTitle)
                .collect(Collectors.toList());
            List<List<Double>> titleVectors = embeddingExecutor.embedTexts(titles);

            log.info("正在提取摘要并向量化...");
            List<String> summaries = contentExtractor.extractBatch(financialArticle);
            List<List<Double>> summaryVectors = embeddingExecutor.embedTexts(summaries);

            for (int i = 0; i < financialArticle.size(); i++) {
                financialArticle.get(i).setTitleVector(titleVectors.get(i));
                financialArticle.get(i).setSummaryVector(summaryVectors.get(i));
            }

            log.info("向量化完成");

        } catch (Exception e) {
            log.error("向量化失败", e);
            throw new RuntimeException("向量化失败", e);
        }
    }

    /**
     * 按需向量化：查询未处理数据并进行向量化
     *
     * @param batchSize 每批处理数量
     * @return 成功处理的数据条数
     */
    public int processUnvectorizedData(int batchSize) {
        log.info("========== 开始按需向量化处理，批次大小：{} ==========", batchSize);

        // 查询未向量化的数据
        List<FinancialArticle> unprocessedData = storageService.findUnprocessed(batchSize);
        if (unprocessedData.isEmpty()) {
            log.info("无需处理，所有数据已向量化");
            return 0;
        }

        // 执行向量化
        embedData(unprocessedData);

        // 更新到ES（包含向量和processed状态）
        int successCount = storageService.batchUpdateVectors(unprocessedData);

        log.info("========== 按需向量化完成，成功处理 {} 条数据 ==========", successCount);
        return successCount;
    }

    /**
     * 处理所有未向量化数据（循环处理直到全部完成）
     *
     * @param batchSize 每批处理数量
     * @return 总共处理的数据条数
     */
    public int processAllUnvectorizedData(int batchSize) {
        log.info("========== 开始处理所有未向量化数据 ==========");

        int totalProcessed = 0;
        int processed;

        do {
            processed = processUnvectorizedData(batchSize);
            totalProcessed += processed;

            if (processed > 0) {
                log.info("已处理 {} 条，累计处理 {} 条", processed, totalProcessed);
            }
        } while (processed > 0);

        log.info("========== 全部处理完成，共处理 {} 条数据 ==========", totalProcessed);
        return totalProcessed;
    }

    /**
     * 处理所有未向量化数据（使用默认批次大小）
     *
     * @return 总共处理的数据条数
     */
    public int processAllUnvectorizedData() {
        return processAllUnvectorizedData(DEFAULT_QUERY_BATCH_SIZE);
    }


    /**
     * 获取未向量化数据数量
     *
     * @return 未向量化数据数量
     */
    public long countUnvectorizedData() {
        return storageService.countUnprocessed();
    }
}


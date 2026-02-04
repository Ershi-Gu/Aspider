package com.ershi.aspider.data.orchestration.service;

import com.ershi.aspider.data.datasource.service.FinancialArticleDSFactory;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.data.datasource.provider.FinancialArticleDataSource;
import com.ershi.aspider.data.embedding.EmbeddingExecutor;
import com.ershi.aspider.data.processor.cleaner.FinancialArticleCleaner;
import com.ershi.aspider.data.processor.scorer.ArticleScorer;
import com.ershi.aspider.data.processor.summary.SummaryProcessor;
import com.ershi.aspider.data.storage.elasticsearch.service.FinancialArticleStorageService;
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

    private final FinancialArticleDSFactory financialArticleDSFactory;
    private final FinancialArticleCleaner financialArticleCleaner;
    private final EmbeddingExecutor embeddingExecutor;
    private final ArticleScorer articleScorer;
    private final SummaryProcessor summaryProcessor;
    private final FinancialArticleStorageService storageService;

    public FinancialArticleDataService(FinancialArticleDSFactory financialArticleDSFactory,
                                       FinancialArticleCleaner financialArticleCleaner,
                                       EmbeddingExecutor embeddingExecutor,
                                       ArticleScorer articleScorer,
                                       SummaryProcessor summaryProcessor,
                                       FinancialArticleStorageService storageService) {
        this.financialArticleDSFactory = financialArticleDSFactory;
        this.financialArticleCleaner = financialArticleCleaner;
        this.embeddingExecutor = embeddingExecutor;
        this.articleScorer = articleScorer;
        this.summaryProcessor = summaryProcessor;
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
     * 处理指定数据源的新闻数据（采集即向量化）
     */
    public int processFinancialArticle(FinancialArticleDSTypeEnum sourceType) {
        log.info("========== 开始处理数据源 [{}] ==========", sourceType.getDesc());

        List<FinancialArticle> financialArticles = fetchFromDataSource(sourceType);
        if (financialArticles.isEmpty()) {
            log.warn("数据源 [{}] 未获取到数据", sourceType.getDesc());
            return 0;
        }

        return executePipeline(financialArticles);
    }

    /**
     * 采集即向量化处理管道
     * <p>
     * 流程：数据清洗 → 重要性评分 → 摘要处理（提取+质量评估+LLM优化） → 向量化 → 持久化
     */
    private int executePipeline(List<FinancialArticle> financialArticle) {
        // 1. 数据清洗
        List<FinancialArticle> cleanedData = financialArticleCleaner.clean(financialArticle);
        if (cleanedData.isEmpty()) {
            log.warn("清洗后无有效数据");
            return 0;
        }
        log.info("[Step 1/4] 数据清洗完成，有效数据 {} 条", cleanedData.size());

        // 2. 重要性评分
        articleScorer.scoreBatch(cleanedData);
        log.info("[Step 2/4] 重要性评分完成");

        // 3. 摘要处理（统一入口：提取 + 质量评估 + LLM优化）
        summaryProcessor.processBatch(cleanedData);
        log.info("[Step 3/4] 摘要处理完成");

        // 4. 向量化 + 持久化
        embedData(cleanedData);
        cleanedData.forEach(item -> item.setProcessed(true));
        int successCount = storageService.batchSaveToEs(cleanedData);
        log.info("[Step 4/4] 向量化与持久化完成，成功保存 {} 条数据", successCount);

        log.info("========== 采集即向量化流程完成 ==========");
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

            log.info("正在向量化摘要...");
            List<String> summaries = financialArticle.stream()
                .map(FinancialArticle::getSummary)
                .collect(Collectors.toList());
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

}


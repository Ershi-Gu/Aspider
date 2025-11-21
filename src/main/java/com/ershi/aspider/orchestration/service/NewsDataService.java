package com.ershi.aspider.orchestration.service;

import com.ershi.aspider.datasource.service.NewsDataFactory;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import com.ershi.aspider.datasource.domain.NewsDataSourceTypeEnum;
import com.ershi.aspider.datasource.provider.NewsDataSource;
import com.ershi.aspider.embedding.EmbeddingExecutor;
import com.ershi.aspider.datasource.domain.NewsDataCleaner;
import com.ershi.aspider.processor.extractor.ContentExtractor;
import com.ershi.aspider.storage.elasticsearch.service.NewsDataStorageService;
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
public class NewsDataService {

    private static final Logger log = LoggerFactory.getLogger(NewsDataService.class);

    private final NewsDataFactory newsDataFactory;
    private final NewsDataCleaner newsDataCleaner;
    private final ContentExtractor contentExtractor;
    private final EmbeddingExecutor embeddingExecutor;
    private final NewsDataStorageService storageService;

    public NewsDataService(NewsDataFactory newsDataFactory, NewsDataCleaner newsDataCleaner,
        ContentExtractor contentExtractor, EmbeddingExecutor embeddingExecutor, NewsDataStorageService storageService) {
        this.newsDataFactory = newsDataFactory;
        this.newsDataCleaner = newsDataCleaner;
        this.contentExtractor = contentExtractor;
        this.embeddingExecutor = embeddingExecutor;
        this.storageService = storageService;
    }

    /**
     * 处理所有数据源的新闻数据
     */
    public int processAllNewsData() {
        log.info("========== 开始处理所有数据源新闻数据 ==========");

        List<NewsDataItem> allNewsData = fetchFromAllDataSources();
        if (allNewsData.isEmpty()) {
            log.warn("未获取到任何新闻数据");
            return 0;
        }

        return executePipeline(allNewsData);
    }

    /**
     * 处理指定数据源的新闻数据
     */
    public int processNewsDataBySource(NewsDataSourceTypeEnum sourceType) {
        log.info("========== 开始处理数据源 [{}] ==========", sourceType.getDesc());

        List<NewsDataItem> newsData = fetchFromDataSource(sourceType);
        if (newsData.isEmpty()) {
            log.warn("数据源 [{}] 未获取到数据", sourceType.getDesc());
            return 0;
        }

        return executePipeline(newsData);
    }

    /**
     * 核心处理管道
     */
    private int executePipeline(List<NewsDataItem> newsData) {
        // 数据清洗
        List<NewsDataItem> cleanedData = newsDataCleaner.clean(newsData);
        if (cleanedData.isEmpty()) {
            log.warn("清洗后无有效数据");
            return 0;
        }

        // 向量化
        embedData(cleanedData);

        // 持久化
        int successCount = storageService.batchSaveToEs(cleanedData);

        log.info("========== 处理完成，成功保存 {} 条数据 ==========", successCount);
        return successCount;
    }

    /**
     * 从所有数据源获取数据
     */
    private List<NewsDataItem> fetchFromAllDataSources() {
        List<NewsDataSource> dataSources = newsDataFactory.getAllDataSources();
        log.info("发现 {} 个数据源", dataSources.size());

        List<NewsDataItem> allData = new ArrayList<>();

        for (NewsDataSource dataSource : dataSources) {
            try {
                String sourceDesc = dataSource.getDataSourceType().getDesc();
                log.info("正在获取数据源 [{}] 的数据...", sourceDesc);

                // 执行数据请求
                List<NewsDataItem> data = dataSource.getNewsData();
                log.info("数据源 [{}] 获取到 {} 条数据", sourceDesc, data.size());

                allData.addAll(data);

            } catch (Exception e) {
                log.error("数据源 [{}] 获取数据失败",
                          dataSource.getDataSourceType().getType(), e);
            }
        }

        log.info("所有数据源共获取 {} 条数据", allData.size());
        return allData;
    }

    /**
     * 从指定数据源获取数据
     */
    private List<NewsDataItem> fetchFromDataSource(NewsDataSourceTypeEnum sourceType) {
        NewsDataSource dataSource = newsDataFactory.getDataSource(sourceType);
        return dataSource.getNewsData();
    }

    /**
     * 向量化处理
     */
    private void embedData(List<NewsDataItem> newsData) {
        log.info("开始向量化 {} 条新闻数据", newsData.size());

        try {
            log.info("正在向量化标题...");
            List<String> titles = newsData.stream()
                .map(NewsDataItem::getTitle)
                .collect(Collectors.toList());
            List<List<Double>> titleVectors = embeddingExecutor.embedTexts(titles);

            log.info("正在提取并向量化内容...");
            List<String> contents = contentExtractor.extractBatch(newsData);
            List<List<Double>> contentVectors = embeddingExecutor.embedTexts(contents);

            for (int i = 0; i < newsData.size(); i++) {
                newsData.get(i).setTitleVector(titleVectors.get(i));
                newsData.get(i).setContentVector(contentVectors.get(i));
            }

            log.info("向量化完成");

        } catch (Exception e) {
            log.error("向量化失败", e);
            throw new RuntimeException("向量化失败", e);
        }
    }
}


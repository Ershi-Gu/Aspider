package com.ershi.aspider.datasource.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ershi.aspider.datasource.NewsDataSource;
import com.ershi.aspider.datasource.domain.NewsDataSourceTypeEnum;
import com.ershi.aspider.datasource.domain.NewsDataClearEsDTO;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import com.ershi.aspider.embedding.EmbeddingExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 新闻数据操作服务
 *
 * @author Ershi-Gu.
 * @since 2025-11-13
 */
@Service
public class NewsDataService {

    public static final Logger log = LoggerFactory.getLogger(NewsDataService.class);

    /** 新闻政策ES索引名 */
    public static final String NEWS_DATA_INDEX = "news_data_items";

    private final NewsDataFactory newsDataFactory;

    private final ElasticsearchClient elasticsearchClient;

    private final EmbeddingExecutor embeddingExecutor;

    NewsDataService(NewsDataSource newsDataSource, NewsDataFactory newsDataFactory,
        ElasticsearchClient elasticsearchClient, EmbeddingExecutor embeddingExecutor) {
        this.newsDataFactory = newsDataFactory;
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingExecutor = embeddingExecutor;
    }

    /**
     * 处理所有数据源新闻数据 <br> 数据获取 -> 清洗 -> 向量化 -> ES存储
     *
     * @return int 成功持久化的数据条数
     */
    public int processAllNewsData() {
        log.info("========== 开始获取新闻数据并持久化 ==========");

        // 从数据源获取数据
        List<NewsDataItem> allNewsData = fetchFromAllDataSources();
        if (allNewsData.isEmpty()) {
            log.warn("未获取到任何新闻数据，流程结束");
            return 0;
        }

        // 数据清洗去重
        List<NewsDataItem> cleanedData = cleanNewsData(allNewsData);
        if (cleanedData.isEmpty()) {
            log.warn("清洗后无有效数据，结束流程");
            return 0;
        }

        // 语义字段向量化
        embedNewsData(cleanedData);

        // 批量保存到ES
        int successCount = batchSaveToEs(cleanedData);

        log.info("========== 持久化完成，成功保存 {} 条数据 ==========", successCount);
        return successCount;
    }

    /**
     * 处理指定数据源的新闻数据
     *
     * @param dataSourceType 数据源类型
     * @return 成功保存的数据条数
     */
    public int processNewsDataBySource(NewsDataSourceTypeEnum newsDataSourceTypeEnum) {
        log.info("========== 开始持久化数据源 [{}] 的新闻数据 ==========", newsDataSourceTypeEnum.getDesc());

        // 从指定数据源获取数据
        List<NewsDataItem> newsData = fetchFromDataSource(newsDataSourceTypeEnum);
        if (newsData.isEmpty()) {
            log.warn("数据源 [{}] 未获取到数据", newsDataSourceTypeEnum.getDesc());
            return 0;
        }

        // 执行清洗、向量化、存储
        return processPipeline(newsData);
    }

    /**
     * 通用数据处理管道：清洗 → 向量化 → 存储
     */
    private int processPipeline(List<NewsDataItem> newsData) {
        // 数据清洗
        List<NewsDataItem> cleanedData = cleanNewsData(newsData);
        if (cleanedData.isEmpty()) {
            return 0;
        }

        // 向量化处理
        embedNewsData(cleanedData);

        // 批量保存
        return batchSaveToEs(cleanedData);
    }

    /**
     * 从所有数据源获取新闻数据
     */
    private List<NewsDataItem> fetchFromAllDataSources() {
        // 获取所有数据源
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
                log.error("数据源 [{}] 获取数据失败", dataSource.getDataSourceType().getType(), e);
            }
        }

        log.info("所有数据源共获取 {} 条数据", allData.size());
        return allData;
    }

    /**
     * 从指定数据源获取数据
     */
    private List<NewsDataItem> fetchFromDataSource(NewsDataSourceTypeEnum newsDataSourceTypeEnum) {
        // 获取指定数据源
        NewsDataSource dataSource = newsDataFactory.getDataSource(newsDataSourceTypeEnum);

        // 请求数据
        return dataSource.getNewsData();
    }

    /**
     * 新闻数据清洗流程
     *
     * @param newsDataItems
     * @return {@link List }<{@link NewsDataItem }>
     */
    private List<NewsDataItem> cleanNewsData(List<NewsDataItem> newsDataItems) {
        log.info("开始数据清洗，原始数据 {} 条", newsDataItems.size());

        // todo 数据验证清洗

        // ES去重
        filterDuplicates(newsDataItems);
        log.info("去重完成，剩余 {} 条新数据", newsDataItems.size());

        return newsDataItems;

    }

    /**
     * 过滤重复新闻数据
     *
     * @param newsData
     */
    public void filterDuplicates(List<NewsDataItem> newsData) {
        // 获取唯一标识列表
        List<String> uniqueIds = newsData.stream()
            .map(NewsDataItem::getUniqueId)
            .collect(Collectors.toList());

        // 检查是否存在，返回已存在标识
        Set<String> existUniqueIds = checkExistingUniqueIds(uniqueIds);

        // 过滤已存在数据
        newsData.removeIf(item -> existUniqueIds.contains(item.getUniqueId()));
    }

    /**
     * 批量检查唯一ID是否已存在
     *
     * @param uniqueIds 唯一ID列表
     * @return 已存在的唯一ID集合
     */
    private Set<String> checkExistingUniqueIds(List<String> uniqueIds) {
        if (uniqueIds == null || uniqueIds.isEmpty()) {
            return Set.of();
        }

        try {
            // 构建查询：查询uniqueId在给定列表中的文档
            Query query = Query.of(q -> q
                .terms(t -> t
                    .field("uniqueId")
                    .terms(tv -> tv.value(
                        uniqueIds.stream()
                            .map(FieldValue::of)
                            .toList()
                    ))
                )
            );

            // 构建搜索请求（只返回uniqueId字段）
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(NEWS_DATA_INDEX)
                .query(query)
                .source(src -> src
                    .filter(f -> f
                        .includes("uniqueId")
                    )
                )
                // 限制返回条数最大为请求条数
                .size(uniqueIds.size())
            );

            // 执行查询
            SearchResponse<NewsDataClearEsDTO> response = elasticsearchClient.search(
                searchRequest,
                NewsDataClearEsDTO.class
            );

            // 提取已存在的uniqueId
            Set<String> existingIds = response.hits().hits().stream()
                .map(Hit::source)
                .filter(item -> item != null && item.getUniqueId() != null)
                .map(NewsDataClearEsDTO::getUniqueId)
                .collect(Collectors.toSet());

            log.info("批量检查 {} 条数据，发现 {} 条已存在", uniqueIds.size(), existingIds.size());
            return existingIds;

        } catch (IOException e) {
            log.error("查询ES失败", e);
            return Set.of();
        }
    }

    /**
     * 向量化新闻数据的标题和内容
     */
    private void embedNewsData(List<NewsDataItem> newsData) {
        log.info("开始向量化 {} 条新闻数据", newsData.size());

        try {
            // 批量向量化标题
            log.info("正在向量化标题...");
            List<String> titles = newsData.stream()
                .map(NewsDataItem::getTitle)
                .collect(Collectors.toList());

            List<List<Double>> titleVectors = embeddingExecutor.embedTexts(titles);

            // 批量向量化内容
            log.info("正在向量化内容...");
            List<String> contents = newsData.stream()
                .map(item -> item.getContent() != null ? item.getContent() : item.getSummary())
                .collect(Collectors.toList());

            // 执行向量化
            List<List<Double>> contentVectors = embeddingExecutor.embedTexts(contents);

            // 将向量设置到对应的NewsDataItem
            for (int i = 0; i < newsData.size(); i++) {
                newsData.get(i).setTitleVector(titleVectors.get(i));
                newsData.get(i).setContentVector(contentVectors.get(i));
            }

        } catch (Exception e) {
            log.error("向量化失败", e);
            throw new RuntimeException("向量化失败", e);
        }
    }

    /**
     * 批量保存到Elasticsearch
     *
     * @param newsData 待保存的新闻数据
     * @return 成功保存的数据条数
     */
    private int batchSaveToEs(List<NewsDataItem> newsData) {
        if (newsData == null || newsData.isEmpty()) {
            log.warn("无数据需要保存");
            return 0;
        }

        log.info("开始批量保存 {} 条数据到ES", newsData.size());

        try {
            // 构建批量请求
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (NewsDataItem item : newsData) {
                bulkBuilder.operations(op -> op
                    .index(idx -> idx
                        .index(NEWS_DATA_INDEX)
                        .id(item.getUniqueId())
                        .document(item)
                    )
                );
            }

            // 执行批量保存
            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());

            // 统计结果
            int successCount = 0;
            int failureCount = 0;

            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    failureCount++;
                    log.error("保存失败 [ID: {}]: {}",
                              item.id(),
                              item.error().reason());
                } else {
                    successCount++;
                }
            }

            log.info("ES批量保存完成，成功: {}，失败: {}", successCount, failureCount);
            return successCount;

        } catch (IOException e) {
            log.error("批量保存到ES失败", e);
            throw new RuntimeException("批量保存到ES失败", e);
        }
    }

}

package com.ershi.aspider.processor.cleaner;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 新闻数据清洗服务
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
@Component
public class NewsDataCleaner {

    private static final Logger log = LoggerFactory.getLogger(NewsDataCleaner.class);
    private static final String NEWS_DATA_INDEX = "news_data_items";

    private final ElasticsearchClient elasticsearchClient;

    public NewsDataCleaner(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 清洗数据：文本清洗 + 去重
     */
    public List<NewsDataItem> clean(List<NewsDataItem> newsData) {
        log.info("开始数据清洗，原始数据 {} 条", newsData.size());

        // 移除空白字符
        removeWhitespace(newsData);

        // 去重
        filterDuplicates(newsData);

        log.info("清洗完成，剩余 {} 条新数据", newsData.size());
        return newsData;
    }

    /**
     * 移除文本中的空白字符（空格、全角空格、不换行空格等）
     */
    public void removeWhitespace(List<NewsDataItem> newsData) {
        for (NewsDataItem item : newsData) {
            if (item.getContent() != null) {
                // 移除所有空白字符（空格、全角空格、不换行空格）
                String cleaned = item.getContent().replaceAll("[\\s\\u3000\\u00A0]+", "");
                item.setContent(cleaned);
            }
        }
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
}

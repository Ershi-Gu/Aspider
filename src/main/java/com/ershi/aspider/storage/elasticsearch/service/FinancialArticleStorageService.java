package com.ershi.aspider.storage.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.ershi.aspider.datasource.domain.FinancialArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 新闻数据存储服务
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
@Service
public class FinancialArticleStorageService {

    private static final Logger log = LoggerFactory.getLogger(FinancialArticleStorageService.class);

    private static final String NEWS_DATA_INDEX = "financial_article";

    private final ElasticsearchClient elasticsearchClient;

    public FinancialArticleStorageService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 批量保存到Elasticsearch
     *
     * @param financialArticle 待保存的新闻数据
     * @return 成功保存的数据条数
     */
    public int batchSaveToEs(List<FinancialArticle> financialArticle) {
        if (financialArticle == null || financialArticle.isEmpty()) {
            log.warn("无数据需要保存");
            return 0;
        }

        log.info("开始批量保存 {} 条数据到ES", financialArticle.size());

        try {
            // 构建批量请求
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (FinancialArticle item : financialArticle) {
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

    /**
     * 删除指定时间之前的过期新闻数据
     *
     * @param beforeTime 删除此时间之前的数据
     * @return 删除的数据条数
     */
    public long deleteByPublishTimeBefore(LocalDateTime beforeTime) {
        String timeStr = beforeTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("开始清理 {} 之前的过期新闻数据", timeStr);

        try {
            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(d -> d
                .index(NEWS_DATA_INDEX)
                .query(q -> q
                    .range(r -> r
                        .date(dr -> dr
                            .field("publishTime")
                            .lt(timeStr)
                        )
                    )
                )
            );

            long deleted = response.deleted() != null ? response.deleted() : 0;
            log.info("过期新闻数据清理完成，共删除 {} 条数据", deleted);
            return deleted;

        } catch (IOException e) {
            log.error("清理过期新闻数据失败", e);
            throw new RuntimeException("清理过期新闻数据失败", e);
        }
    }
}

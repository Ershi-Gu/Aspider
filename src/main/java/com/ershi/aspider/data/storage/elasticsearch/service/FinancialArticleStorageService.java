package com.ershi.aspider.data.storage.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
     * 查询最近N天内的新闻数据
     *
     * @param days 天数
     * @param size 查询数量限制
     * @return 新闻列表
     */
    public List<FinancialArticle> findRecentByDays(int days, int size) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        log.info("开始查询最近 {} 天的新闻数据，起始时间：{}", days, startTime);

        try {
            SearchResponse<FinancialArticle> response = elasticsearchClient.search(s -> s
                    .index(NEWS_DATA_INDEX)
                    .query(q -> q
                        .range(r -> r
                            .date(dr -> dr
                                .field("publishTime")
                                .gte(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            )
                        )
                    )
                    .size(size)
                    .sort(so -> so
                        .field(f -> f
                            .field("publishTime")
                            .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                        )
                    ),
                FinancialArticle.class
            );

            List<FinancialArticle> result = new ArrayList<>();
            for (Hit<FinancialArticle> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    FinancialArticle article = hit.source();
                    article.setUniqueId(hit.id());
                    result.add(article);
                }
            }

            log.info("查询到 {} 条新闻数据", result.size());
            return result;

        } catch (IOException e) {
            log.error("查询新闻数据失败", e);
            throw new RuntimeException("查询新闻数据失败", e);
        }
    }

    /**
     * 分层清理过期新闻数据
     * <p>
     * 清理策略：
     * - 90天前且重要性 < 3 的普通新闻：删除
     * - 90天前且重要性 >= 3 的重要新闻：保留
     *
     * @param beforeTime 清理此时间之前的数据
     * @param minImportance 最低保留重要性（低于此值的删除）
     * @return 删除的数据条数
     */
    public long deleteByTimeAndImportance(LocalDateTime beforeTime, int minImportance) {
        String timeStr = beforeTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("开始分层清理 {} 之前、重要性 < {} 的过期新闻数据", timeStr, minImportance);

        try {
            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(d -> d
                .index(NEWS_DATA_INDEX)
                .query(q -> q
                    .bool(b -> b
                        // 时间条件：指定时间之前
                        .must(m -> m
                            .range(r -> r
                                .date(dr -> dr
                                    .field("crawlTime")
                                    .lt(timeStr)
                                )
                            )
                        )
                        // 重要性条件：低于阈值 或 字段不存在
                        .must(m -> m
                            .bool(ib -> ib
                                .should(sh -> sh
                                    .range(r -> r
                                        .number(nr -> nr
                                            .field("importance")
                                            .lt((double) minImportance)
                                        )
                                    )
                                )
                                .should(sh -> sh
                                    .bool(nb -> nb
                                        .mustNot(mn -> mn
                                            .exists(e -> e.field("importance"))
                                        )
                                    )
                                )
                                .minimumShouldMatch("1")
                            )
                        )
                    )
                )
            );

            long deleted = response.deleted() != null ? response.deleted() : 0;
            log.info("分层清理完成，共删除 {} 条低重要性过期数据", deleted);
            return deleted;

        } catch (IOException e) {
            log.error("分层清理过期新闻数据失败", e);
            throw new RuntimeException("分层清理过期新闻数据失败", e);
        }
    }
}

package com.ershi.aspider.storage.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ershi.aspider.datasource.domain.FinancialArticle;
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

    /**
     * 查询未向量化的新闻数据
     *
     * @param size 查询数量
     * @return 未向量化的新闻数据列表
     */
    public List<FinancialArticle> findUnprocessed(int size) {
        log.info("开始查询未向量化的新闻数据，数量限制：{}", size);

        try {
            SearchResponse<FinancialArticle> response = elasticsearchClient.search(s -> s
                    .index(NEWS_DATA_INDEX)
                    .query(q -> q
                        .bool(b -> b
                            .should(sh -> sh
                                .term(t -> t
                                    .field("processed")
                                    .value(false)
                                )
                            )
                            .should(sh -> sh
                                .bool(nb -> nb
                                    .mustNot(mn -> mn
                                        .exists(e -> e.field("processed"))
                                    )
                                )
                            )
                            .minimumShouldMatch("1")
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

            log.info("查询到 {} 条未向量化数据", result.size());
            return result;

        } catch (IOException e) {
            log.error("查询未向量化数据失败", e);
            throw new RuntimeException("查询未向量化数据失败", e);
        }
    }

    /**
     * 批量更新向量化状态和向量数据
     *
     * @param articles 已向量化的新闻数据（包含向量）
     * @return 成功更新的数据条数
     */
    public int batchUpdateVectors(List<FinancialArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            log.warn("无数据需要更新");
            return 0;
        }

        log.info("开始批量更新 {} 条数据的向量", articles.size());

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (FinancialArticle article : articles) {
                // 标记为已处理
                article.setProcessed(true);

                bulkBuilder.operations(op -> op
                    .update(u -> u
                        .index(NEWS_DATA_INDEX)
                        .id(article.getUniqueId())
                        .action(a -> a
                            .doc(article)
                        )
                    )
                );
            }

            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());

            int successCount = 0;
            int failureCount = 0;

            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    failureCount++;
                    log.error("更新失败 [ID: {}]: {}", item.id(), item.error().reason());
                } else {
                    successCount++;
                }
            }

            log.info("向量更新完成，成功: {}，失败: {}", successCount, failureCount);
            return successCount;

        } catch (IOException e) {
            log.error("批量更新向量失败", e);
            throw new RuntimeException("批量更新向量失败", e);
        }
    }

    /**
     * 统计未向量化的数据数量
     *
     * @return 未向量化数据数量
     */
    public long countUnprocessed() {
        try {
            SearchResponse<FinancialArticle> response = elasticsearchClient.search(s -> s
                    .index(NEWS_DATA_INDEX)
                    .query(q -> q
                        .bool(b -> b
                            .should(sh -> sh
                                .term(t -> t
                                    .field("processed")
                                    .value(false)
                                )
                            )
                            .should(sh -> sh
                                .bool(nb -> nb
                                    .mustNot(mn -> mn
                                        .exists(e -> e.field("processed"))
                                    )
                                )
                            )
                            .minimumShouldMatch("1")
                        )
                    )
                    .size(0)
                    .trackTotalHits(t -> t.enabled(true)),
                FinancialArticle.class
            );

            long count = response.hits().total() != null ? response.hits().total().value() : 0;
            log.info("未向量化数据数量：{}", count);
            return count;

        } catch (IOException e) {
            log.error("统计未向量化数据失败", e);
            throw new RuntimeException("统计未向量化数据失败", e);
        }
    }
}

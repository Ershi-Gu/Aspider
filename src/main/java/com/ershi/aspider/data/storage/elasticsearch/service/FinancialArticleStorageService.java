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

import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;

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

    /** 向量检索最低相似度分数阈值 */
    private static final double MIN_SCORE_THRESHOLD = 0.6;

    /** 混合检索最低相似度分数阈值（归一化后的分数，范围0-1） */
    private static final double HYBRID_SCORE_THRESHOLD = 0.6;

    /** 混合检索分数归一化基准值（经验值，用于将原始分数归一化到0-1范围，双向量检索后适当提高） */
    private static final double HYBRID_SCORE_NORMALIZATION_BASE = 25.0;

    /** 混合检索中摘要向量检索的权重 */
    private static final float SUMMARY_VECTOR_BOOST = 2.0f;

    /** 混合检索中标题向量检索的权重 */
    private static final float TITLE_VECTOR_BOOST = 1.5f;

    /** 混合检索中标题关键词匹配的权重 */
    private static final float TITLE_BOOST = 3.0f;

    /** 混合检索中摘要匹配的权重 */
    private static final float SUMMARY_BOOST = 1.5f;

    /** 重要新闻的boost权重 */
    private static final float IMPORTANCE_BOOST = 1.5f;

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
        log.info("开始查询最近 {} 天的新闻数据", days);

        try {
            String timeStr = formatTimeFilter(days);

            SearchResponse<FinancialArticle> response = elasticsearchClient.search(s -> s
                    .index(NEWS_DATA_INDEX)
                    .query(q -> q
                        .range(r -> r
                            .date(dr -> dr
                                .field("publishTime")
                                .gte(timeStr)
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

            List<FinancialArticle> result = extractArticlesFromResponse(response);
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

    /**
     * 向量KNN语义检索
     * <p>
     * 基于summaryVector进行KNN近邻搜索，返回语义相关的新闻
     *
     * @param queryVector 查询向量
     * @param topK        返回数量
     * @param days        时间范围（最近N天，0表示不限制）
     * @return 语义相关的新闻列表
     */
    public List<FinancialArticle> searchByVector(List<Float> queryVector, int topK, int days) {
        log.info("开始向量KNN检索，topK={}, days={}, 最低分数阈值={}", topK, days, MIN_SCORE_THRESHOLD);

        try {
            SearchResponse<FinancialArticle> response;

            if (days > 0) {
                // 带时间过滤的KNN检索
                String timeStr = formatTimeFilter(days);

                response = elasticsearchClient.search(s -> s
                        .index(NEWS_DATA_INDEX)
                        .knn(k -> k
                            .field("summaryVector")
                            .queryVector(queryVector)
                            .k(topK * 5)  // 返回5倍结果，为后置过滤预留空间
                            .numCandidates(topK * 20)  // HNSW算法搜索时考察20倍候选，提高召回质量
                            .filter(f -> f
                                .range(r -> r
                                    .date(dr -> dr
                                        .field("publishTime")
                                        .gte(timeStr)
                                    )
                                )
                            )
                        ),
                    FinancialArticle.class
                );
            } else {
                // 不带时间过滤的KNN检索
                response = elasticsearchClient.search(s -> s
                        .index(NEWS_DATA_INDEX)
                        .knn(k -> k
                            .field("summaryVector")
                            .queryVector(queryVector)
                            .k(topK * 5)  // 返回5倍结果，为后置过滤预留空间
                            .numCandidates(topK * 20)  // HNSW算法搜索时考察20倍候选，提高召回质量
                        ),
                    FinancialArticle.class
                );
            }

            // 使用通用方法过滤结果
            List<FinancialArticle> result = filterAndLimitResults(response, topK, MIN_SCORE_THRESHOLD);

            log.info("向量KNN检索完成，候选 {} 条，过滤后返回 {} 条结果",
                response.hits().hits().size(), result.size());
            return result;

        } catch (IOException e) {
            log.error("向量KNN检索失败", e);
            throw new RuntimeException("向量KNN检索失败", e);
        }
    }

    /**
     * 按新闻类型和时间范围查询
     *
     * @param newsType 新闻类型
     * @param days     最近N天
     * @param size     数量限制
     * @return 新闻列表
     */
    public List<FinancialArticle> findByNewsTypeAndDays(NewsTypeEnum newsType, int days, int size) {
        log.info("查询{}类型新闻，最近{}天，限制{}条", newsType.getDescription(), days, size);

        try {
            String timeStr = formatTimeFilter(days);

            SearchResponse<FinancialArticle> response = elasticsearchClient.search(s -> s
                    .index(NEWS_DATA_INDEX)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m
                                .term(t -> t
                                    .field("newsType")
                                    .value(newsType.getCode())
                                )
                            )
                            .must(m -> m
                                .range(r -> r
                                    .date(dr -> dr
                                        .field("publishTime")
                                        .gte(timeStr)
                                    )
                                )
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

            List<FinancialArticle> result = extractArticlesFromResponse(response);
            log.info("查询到 {} 条{}类型新闻", result.size(), newsType.getDescription());
            return result;

        } catch (IOException e) {
            log.error("按类型查询新闻失败", e);
            throw new RuntimeException("按类型查询新闻失败", e);
        }
    }

    /**
     * 按重要性查询新闻
     *
     * @param minImportance 最低重要性
     * @param days          最近N天
     * @param size          数量限制
     * @return 新闻列表
     */
    public List<FinancialArticle> findByImportanceAndDays(int minImportance, int days, int size) {
        log.info("查询重要性>={}的新闻，最近{}天，限制{}条", minImportance, days, size);

        try {
            String timeStr = formatTimeFilter(days);

            SearchResponse<FinancialArticle> response = elasticsearchClient.search(s -> s
                    .index(NEWS_DATA_INDEX)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m
                                .range(r -> r
                                    .number(nr -> nr
                                        .field("importance")
                                        .gte((double) minImportance)
                                    )
                                )
                            )
                            .must(m -> m
                                .range(r -> r
                                    .date(dr -> dr
                                        .field("publishTime")
                                        .gte(timeStr)
                                    )
                                )
                            )
                        )
                    )
                    .size(size)
                    .sort(so -> so
                        .field(f -> f
                            .field("importance")
                            .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                        )
                    )
                    .sort(so -> so
                        .field(f -> f
                            .field("publishTime")
                            .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                        )
                    ),
                FinancialArticle.class
            );

            List<FinancialArticle> result = extractArticlesFromResponse(response);
            log.info("查询到 {} 条重要新闻", result.size());
            return result;

        } catch (IOException e) {
            log.error("按重要性查询新闻失败", e);
            throw new RuntimeException("按重要性查询新闻失败", e);
        }
    }

    /**
     * 混合检索：向量语义检索 + 关键词匹配
     * <p>
     * 结合向量相似度和关键词匹配，提高检索准确性
     *
     * @param queryText   查询文本（用于关键词匹配）
     * @param queryVector 查询向量（用于语义检索）
     * @param topK        返回数量
     * @param days        时间范围（最近N天，0表示不限制）
     * @return 相关新闻列表
     */
    public List<FinancialArticle> hybridSearch(String queryText, List<Float> queryVector, int topK, int days) {
        log.info("开始混合检索，查询词={}, topK={}, days={}", queryText, topK, days);

        try {
            SearchResponse<FinancialArticle> response;

            if (days > 0) {
                // 带时间过滤的混合检索
                String timeStr = formatTimeFilter(days);

                response = elasticsearchClient.search(s -> s
                        .index(NEWS_DATA_INDEX)
                        // 关键词查询部分
                        .query(q -> q
                            .bool(b -> b
                                // 标题匹配（高权重）
                                .should(sh -> sh
                                    .match(m -> m
                                        .field("title")
                                        .query(queryText)
                                        .boost(TITLE_BOOST)
                                    )
                                )
                                // 摘要匹配（中权重）
                                .should(sh -> sh
                                    .match(m -> m
                                        .field("summary")
                                        .query(queryText)
                                        .boost(SUMMARY_BOOST)
                                    )
                                )
                                // 重要新闻加权
                                .should(sh -> sh
                                    .range(r -> r
                                        .number(nr -> nr
                                            .field("importance")
                                            .gte(3.0)
                                            .boost(IMPORTANCE_BOOST)
                                        )
                                    )
                                )
                                // 时间过滤
                                .filter(f -> f
                                    .range(r -> r
                                        .date(dr -> dr
                                            .field("publishTime")
                                            .gte(timeStr)
                                        )
                                    )
                                )
                            )
                        )
                        // 摘要向量检索（主要语义匹配）
                        .knn(k -> k
                            .field("summaryVector")
                            .queryVector(queryVector)
                            .k(topK * 5)
                            .numCandidates(topK * 20)
                            .boost(SUMMARY_VECTOR_BOOST)
                        )
                        // 标题向量检索（补充语义匹配，提高召回率）
                        .knn(k -> k
                            .field("titleVector")
                            .queryVector(queryVector)
                            .k(topK * 3)
                            .numCandidates(topK * 10)
                            .boost(TITLE_VECTOR_BOOST)
                        ),
                    FinancialArticle.class
                );
            } else {
                // 不带时间过滤的混合检索
                response = elasticsearchClient.search(s -> s
                        .index(NEWS_DATA_INDEX)
                        .query(q -> q
                            .bool(b -> b
                                .should(sh -> sh
                                    .match(m -> m
                                        .field("title")
                                        .query(queryText)
                                        .boost(TITLE_BOOST)
                                    )
                                )
                                .should(sh -> sh
                                    .match(m -> m
                                        .field("summary")
                                        .query(queryText)
                                        .boost(SUMMARY_BOOST)
                                    )
                                )
                                .should(sh -> sh
                                    .range(r -> r
                                        .number(nr -> nr
                                            .field("importance")
                                            .gte(3.0)
                                            .boost(IMPORTANCE_BOOST)
                                        )
                                    )
                                )
                            )
                        )
                        // 摘要向量检索（主要语义匹配）
                        .knn(k -> k
                            .field("summaryVector")
                            .queryVector(queryVector)
                            .k(topK * 5)
                            .numCandidates(topK * 20)
                            .boost(SUMMARY_VECTOR_BOOST)
                        )
                        // 标题向量检索（补充语义匹配，提高召回率）
                        .knn(k -> k
                            .field("titleVector")
                            .queryVector(queryVector)
                            .k(topK * 3)
                            .numCandidates(topK * 10)
                            .boost(TITLE_VECTOR_BOOST)
                        ),
                    FinancialArticle.class
                );
            }

            // 使用归一化方法处理混合检索结果
            List<FinancialArticle> result = filterAndLimitResultsWithNormalization(response, topK, HYBRID_SCORE_THRESHOLD);

            log.info("混合检索完成，候选 {} 条，过滤后返回 {} 条结果",
                response.hits().hits().size(), result.size());
            return result;

        } catch (IOException e) {
            log.error("混合检索失败", e);
            throw new RuntimeException("混合检索失败", e);
        }
    }

    /**
     * 通用方法：过滤并限制搜索结果（带固定基准归一化）
     * <p>
     * 使用固定基准值将分数归一化到0-1范围，反映绝对相关性而非相对排名
     *
     * @param response       ES搜索响应
     * @param topK           最大返回数量
     * @param scoreThreshold 分数阈值（0-1范围）
     * @return 过滤后的新闻列表
     */
    private List<FinancialArticle> filterAndLimitResultsWithNormalization(SearchResponse<FinancialArticle> response, int topK, double scoreThreshold) {
        List<FinancialArticle> result = new ArrayList<>();

        for (Hit<FinancialArticle> hit : response.hits().hits()) {
            if (hit.source() != null && hit.score() != null) {
                FinancialArticle article = hit.source();
                article.setUniqueId(hit.id());

                // 使用固定基准归一化分数到0-1范围
                double normalizedScore = Math.min(hit.score() / HYBRID_SCORE_NORMALIZATION_BASE, 1.0);

                // 输出归一化后的分数用于诊断
                log.info("候选文章: {} | 原始分数: {} | 归一化分数: {}",
                    article.getTitle().substring(0, Math.min(30, article.getTitle().length())),
                    hit.score(),
                    String.format("%.3f", normalizedScore));

                // 过滤低分结果
                if (normalizedScore >= scoreThreshold) {
                    result.add(article);

                    // 达到topK就停止
                    if (result.size() >= topK) {
                        break;
                    }
                } else {
                    log.debug("过滤低分文章: {} | 归一化分数: {} < {}",
                        article.getTitle().substring(0, Math.min(30, article.getTitle().length())),
                        String.format("%.3f", normalizedScore), scoreThreshold);
                }
            }
        }

        return result;
    }

    /**
     * 通用方法：过滤并限制搜索结果
     * <p>
     * 根据分数阈值过滤结果，并限制返回数量
     *
     * @param response       ES搜索响应
     * @param topK           最大返回数量
     * @param scoreThreshold 分数阈值
     * @return 过滤后的新闻列表
     */
    private List<FinancialArticle> filterAndLimitResults(SearchResponse<FinancialArticle> response, int topK, double scoreThreshold) {
        List<FinancialArticle> result = new ArrayList<>();

        for (Hit<FinancialArticle> hit : response.hits().hits()) {
            if (hit.source() != null && hit.score() != null) {
                FinancialArticle article = hit.source();
                article.setUniqueId(hit.id());
                Double score = hit.score();

                // 输出相似度分数用于诊断
                log.info("候选文章: {} | 相似度分数: {}",
                    article.getTitle().substring(0, Math.min(30, article.getTitle().length())),
                    score);

                // 过滤低分结果
                if (score >= scoreThreshold) {
                    result.add(article);

                    // 达到topK就停止
                    if (result.size() >= topK) {
                        break;
                    }
                } else {
                    log.debug("过滤低分文章: {} | 分数: {} < {}",
                        article.getTitle().substring(0, Math.min(30, article.getTitle().length())),
                        score, scoreThreshold);
                }
            }
        }

        return result;
    }

    /**
     * 通用方法：格式化时间过滤条件
     *
     * @param days 最近N天
     * @return 格式化后的时间字符串
     */
    private String formatTimeFilter(int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 通用方法：从ES响应中提取文章列表（不带分数过滤）
     *
     * @param response ES搜索响应
     * @return 文章列表
     */
    private List<FinancialArticle> extractArticlesFromResponse(SearchResponse<FinancialArticle> response) {
        List<FinancialArticle> result = new ArrayList<>();
        for (Hit<FinancialArticle> hit : response.hits().hits()) {
            if (hit.source() != null) {
                FinancialArticle article = hit.source();
                article.setUniqueId(hit.id());
                result.add(article);
            }
        }
        return result;
    }
}

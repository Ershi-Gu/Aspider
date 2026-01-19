package com.ershi.aspider.data.storage.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 板块资金流向存储服务
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Service
public class SectorMoneyFlowStorageService {

    private static final Logger log = LoggerFactory.getLogger(SectorMoneyFlowStorageService.class);

    private static final String INDEX_NAME = "sector_money_flow";

    private final ElasticsearchClient elasticsearchClient;

    public SectorMoneyFlowStorageService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 批量保存到Elasticsearch
     *
     * @param dataList 待保存的资金流向数据
     * @return 成功保存的数据条数
     */
    public int batchSaveToEs(List<SectorMoneyFlow> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("无数据需要保存");
            return 0;
        }

        log.info("开始批量保存 {} 条板块资金流向数据到ES", dataList.size());

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (SectorMoneyFlow item : dataList) {
                bulkBuilder.operations(op -> op
                    .index(idx -> idx
                        .index(INDEX_NAME)
                        .id(item.getUniqueId())
                        .document(item)
                    )
                );
            }

            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());

            int successCount = 0;
            int failureCount = 0;

            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    failureCount++;
                    log.error("保存失败 [ID: {}]: {}", item.id(), item.error().reason());
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
     * 查询指定日期的所有板块资金流向
     *
     * @param tradeDate 交易日期
     * @return {@link List }<{@link SectorMoneyFlow }>
     */
    public List<SectorMoneyFlow> findByTradeDate(LocalDate tradeDate) {
        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                    .size(1000),
                SectorMoneyFlow.class
            );

            List<SectorMoneyFlow> result = new ArrayList<>();
            for (Hit<SectorMoneyFlow> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到 {} 条 {} 的板块资金流向数据", result.size(), tradeDate);
            return result;

        } catch (IOException e) {
            log.error("查询板块资金流向失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据板块代码查询资金流向
     *
     * @param sectorCode 板块代码
     * @param limit      返回条数
     * @return {@link List }<{@link SectorMoneyFlow }>
     */
    public List<SectorMoneyFlow> findBySectorCode(String sectorCode, int limit) {
        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("sectorCode").value(sectorCode)))
                    .size(limit),
                SectorMoneyFlow.class
            );

            List<SectorMoneyFlow> result = new ArrayList<>();
            for (Hit<SectorMoneyFlow> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到板块 {} 的 {} 条资金流向数据", sectorCode, result.size());
            return result;

        } catch (IOException e) {
            log.error("查询板块资金流向失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询当日主力净流入Top N板块
     *
     * @param tradeDate 交易日期
     * @param topN      数量
     * @return 按主力净流入降序排列的板块列表
     */
    public List<SectorMoneyFlow> findTopByMainNetInflow(LocalDate tradeDate, int topN) {
        log.info("查询{}主力净流入Top{}板块", tradeDate, topN);

        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                    .size(topN)
                    .sort(sort -> sort.field(f -> f
                        .field("mainNetInflow")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                    )),
                SectorMoneyFlow.class
            );

            List<SectorMoneyFlow> result = new ArrayList<>();
            for (Hit<SectorMoneyFlow> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到Top{}板块，共{}条", topN, result.size());
            return result;

        } catch (IOException e) {
            log.error("查询Top板块失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询当日涨幅Top N板块
     *
     * @param tradeDate 交易日期
     * @param topN      数量
     * @return 按涨幅降序排列的板块列表
     */
    public List<SectorMoneyFlow> findTopByChangePercent(LocalDate tradeDate, int topN) {
        log.info("查询{}涨幅Top{}板块", tradeDate, topN);

        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                    .size(topN)
                    .sort(sort -> sort.field(f -> f
                        .field("changePercent")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                    )),
                SectorMoneyFlow.class
            );

            List<SectorMoneyFlow> result = new ArrayList<>();
            for (Hit<SectorMoneyFlow> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到涨幅Top{}板块，共{}条", topN, result.size());
            return result;

        } catch (IOException e) {
            log.error("查询涨幅Top板块失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询板块连续N日资金流向（按日期倒序）
     *
     * @param sectorCode 板块代码
     * @param days       天数
     * @return 按日期降序排列的资金流向列表
     */
    public List<SectorMoneyFlow> findRecentBySectorCode(String sectorCode, int days) {
        log.info("查询板块{}最近{}日资金流向", sectorCode, days);

        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("sectorCode").value(sectorCode)))
                    .size(days)
                    .sort(sort -> sort.field(f -> f
                        .field("tradeDate")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                    )),
                SectorMoneyFlow.class
            );

            List<SectorMoneyFlow> result = new ArrayList<>();
            for (Hit<SectorMoneyFlow> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到板块{}的{}条资金流向数据", sectorCode, result.size());
            return result;

        } catch (IOException e) {
            log.error("查询板块资金流向失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据板块名称查询当日资金流向
     *
     * @param sectorName 板块名称
     * @param tradeDate  交易日期
     * @return 板块资金流向，未找到返回null
     */
    public SectorMoneyFlow findBySectorNameAndDate(String sectorName, LocalDate tradeDate) {
        log.info("查询板块[{}]在{}的资金流向", sectorName, tradeDate);

        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.match(mt -> mt.field("sectorName").query(sectorName)))
                            .must(m -> m.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                        )
                    )
                    .size(1),
                SectorMoneyFlow.class
            );

            if (!response.hits().hits().isEmpty() && response.hits().hits().get(0).source() != null) {
                SectorMoneyFlow result = response.hits().hits().get(0).source();
                log.info("找到板块[{}]的资金流向数据", sectorName);
                return result;
            }

            log.info("未找到板块[{}]的资金流向数据", sectorName);
            return null;

        } catch (IOException e) {
            log.error("查询板块资金流向失败", e);
            return null;
        }
    }

    /**
     * 计算指定板块在当日的资金流入排名
     *
     * @param sectorCode 板块代码
     * @param tradeDate  交易日期
     * @return 排名（从1开始），未找到返回-1
     */
    public int calculateInflowRank(String sectorCode, LocalDate tradeDate) {
        try {
            // 查询当日所有板块，按主力净流入降序
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                    .size(1000)
                    .sort(sort -> sort.field(f -> f
                        .field("mainNetInflow")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                    )),
                SectorMoneyFlow.class
            );

            int rank = 1;
            for (Hit<SectorMoneyFlow> hit : response.hits().hits()) {
                if (hit.source() != null && sectorCode.equals(hit.source().getSectorCode())) {
                    return rank;
                }
                rank++;
            }

            return -1;

        } catch (IOException e) {
            log.error("计算排名失败", e);
            return -1;
        }
    }

    /**
     * 获取当日板块总数
     *
     * @param tradeDate 交易日期
     * @return 板块总数
     */
    public long countByTradeDate(LocalDate tradeDate) {
        try {
            SearchResponse<SectorMoneyFlow> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                    .size(0),
                SectorMoneyFlow.class
            );

            return response.hits().total() != null ? response.hits().total().value() : 0;

        } catch (IOException e) {
            log.error("统计板块数量失败", e);
            return 0;
        }
    }
}

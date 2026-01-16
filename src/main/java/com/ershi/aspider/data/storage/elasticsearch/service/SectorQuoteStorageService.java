package com.ershi.aspider.data.storage.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 板块行情存储服务
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Service
public class SectorQuoteStorageService {

    private static final Logger log = LoggerFactory.getLogger(SectorQuoteStorageService.class);

    private static final String INDEX_NAME = "sector_quote";

    private final ElasticsearchClient elasticsearchClient;

    public SectorQuoteStorageService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 批量保存到Elasticsearch
     *
     * @param dataList 待保存的行情数据
     * @return 成功保存的数据条数
     */
    public int batchSaveToEs(List<SectorQuote> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("无数据需要保存");
            return 0;
        }

        log.info("开始批量保存 {} 条板块行情数据到ES", dataList.size());

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (SectorQuote item : dataList) {
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
     * 查询指定日期的所有板块行情
     *
     * @param tradeDate 交易日期
     * @return {@link List }<{@link SectorQuote }>
     */
    public List<SectorQuote> findByTradeDate(LocalDate tradeDate) {
        try {
            SearchResponse<SectorQuote> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("tradeDate").value(tradeDate.toString())))
                    .size(1000),
                SectorQuote.class
            );

            List<SectorQuote> result = new ArrayList<>();
            for (Hit<SectorQuote> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到 {} 条 {} 的板块行情数据", result.size(), tradeDate);
            return result;

        } catch (IOException e) {
            log.error("查询板块行情失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据板块代码查询行情
     *
     * @param sectorCode 板块代码
     * @param limit      返回条数
     * @return {@link List }<{@link SectorQuote }>
     */
    public List<SectorQuote> findBySectorCode(String sectorCode, int limit) {
        try {
            SearchResponse<SectorQuote> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("sectorCode").value(sectorCode)))
                    .sort(sort -> sort.field(f -> f.field("tradeDate").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                    .size(limit),
                SectorQuote.class
            );

            List<SectorQuote> result = new ArrayList<>();
            for (Hit<SectorQuote> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }

            log.info("查询到板块 {} 的 {} 条行情数据", sectorCode, result.size());
            return result;

        } catch (IOException e) {
            log.error("查询板块行情失败", e);
            return new ArrayList<>();
        }
    }
}
package com.ershi.aspider.analysis.retriever.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 综合检索结果
 * <p>
 * 包含新闻检索结果和板块数据检索结果。
 *
 * @author Ershi-Gu
 */
@Data
public class RetrievalResult {

    /** 原始查询请求 */
    private AnalysisQuery query;

    /** 新闻检索结果 */
    private NewsRetrievalResult newsResult;

    /** 板块数据检索结果 */
    private SectorDataResult sectorResult;

    /** 检索完成时间 */
    private LocalDateTime retrievalTime;
}

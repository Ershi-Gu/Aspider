package com.ershi.aspider.analysis.retriever.domain;

import com.ershi.aspider.analysis.retriever.domain.enums.AnalysisType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 分析查询请求
 * <p>
 * 封装板块分析所需的查询参数，支持按板块名称或代码检索相关新闻和数据。
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisQuery {

    /** 板块名称（如"半导体"） */
    private String sectorName;

    /** 板块代码（如"BK0477"） */
    private String sectorCode;

    /** 交易日期（默认今日） */
    @Builder.Default
    private LocalDate tradeDate = LocalDate.now();

    /** 新闻检索天数（默认7天） */
    @Builder.Default
    private int newsDays = 7;

    /** 趋势分析天数（默认5天） */
    @Builder.Default
    private int trendDays = 5;

    /** 新闻返回数量（默认20条） */
    @Builder.Default
    private int newsTopK = 20;

    /** 分析类型 */
    private AnalysisType analysisType;
}

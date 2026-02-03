package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * L3 详细分析（3分钟深度）
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailAnalysis {

    /** 消息面分析 */
    private NewsAnalysisDetail newsAnalysis;

    /** 资金面分析 */
    private CapitalAnalysisDetail capitalAnalysis;

    /** 趋势研判分析 */
    private TrendAnalysisDetail trendAnalysis;
}

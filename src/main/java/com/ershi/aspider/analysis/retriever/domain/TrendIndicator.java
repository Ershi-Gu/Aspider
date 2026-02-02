package com.ershi.aspider.analysis.retriever.domain;

import lombok.Data;

/**
 * 趋势指标
 * <p>
 * 基于近N日资金流向计算的板块趋势指标。
 *
 * @author Ershi-Gu
 */
@Data
public class TrendIndicator {

    /** 连续资金流入天数 */
    private int consecutiveInflowDays;

    /** 累计资金流入（元） */
    private double totalInflow;

    /** 平均涨跌幅（%） */
    private double avgChangePercent;

    /** 趋势方向 */
    private TrendDirection direction;
}

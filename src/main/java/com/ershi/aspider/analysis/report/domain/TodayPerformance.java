package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 今日表现数据
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayPerformance {

    /** 涨跌幅 */
    private BigDecimal changePercent;

    /** 排名 */
    private int rank;

    /** 板块总数 */
    private long totalSectors;

    /** 领涨股名称 */
    private String leadStockName;

    /** 换手率 */
    private BigDecimal turnoverRate;
}

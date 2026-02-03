package com.ershi.aspider.analysis.report.domain;

import com.ershi.aspider.analysis.agent.domain.DimensionSignals;
import com.ershi.aspider.analysis.agent.domain.OverallRating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * L1 信号卡片（5秒决策）
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalCard {

    /** 板块名称 */
    private String sectorName;

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 综合评级 */
    private OverallRating overallRating;

    /** 综合评分 0-100 */
    private int overallScore;

    /** 四维信号灯 */
    private DimensionSignals dimensionSignals;

    /** 一句话总结 */
    private String summary;
}

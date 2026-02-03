package com.ershi.aspider.analysis.report.domain;

import com.ershi.aspider.analysis.agent.domain.CapitalStructure;
import com.ershi.aspider.analysis.agent.domain.SignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资金面详细分析
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalAnalysisDetail {

    /** 资金面信号 */
    private SignalType capitalSignal;

    /** 情绪面信号 */
    private SignalType sentimentSignal;

    /** 主力净流入（元） */
    private BigDecimal mainNetInflow;

    /** 连续流入天数 */
    private int consecutiveInflowDays;

    /** 超大单占比（%） */
    private BigDecimal superLargeRatio;

    /** 当日排名 */
    private int inflowRank;

    /** 板块总数 */
    private long totalSectors;

    /** 资金结构详情 */
    private CapitalStructure capitalStructure;

    /** 热度评分 0-100 */
    private int heatScore;
}

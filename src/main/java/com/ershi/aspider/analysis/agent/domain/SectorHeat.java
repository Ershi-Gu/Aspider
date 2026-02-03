package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SectorAgent 输出结果 - 板块热度分析
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorHeat {

    /** 资金面信号 */
    private SignalType capitalSignal;

    /** 情绪面信号（基于超大单占比判断机构参与度） */
    private SignalType sentimentSignal;

    /** 主力净流入金额（元） */
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

    /** 分析状态 */
    private AnalysisStatus status;

    /**
     * 创建空结果（降级时使用）
     */
    public static SectorHeat empty(String reason) {
        return SectorHeat.builder()
            .capitalSignal(SignalType.NEUTRAL)
            .sentimentSignal(SignalType.NEUTRAL)
            .mainNetInflow(BigDecimal.ZERO)
            .consecutiveInflowDays(0)
            .superLargeRatio(BigDecimal.ZERO)
            .inflowRank(-1)
            .totalSectors(0)
            .capitalStructure(null)
            .heatScore(0)
            .status(AnalysisStatus.degraded(reason))
            .build();
    }
}

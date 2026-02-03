package com.ershi.aspider.analysis.agent.core;

import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.analysis.retriever.domain.TrendIndicator;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 资金面分析Agent（规则驱动，不调用LLM）
 *
 * 从板块资金流向数据中提取资金面信号、情绪面信号和热度评分
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class SectorAgent implements Agent<AgentContext, SectorHeat> {

    /** 前N%为正向信号的排名阈值 */
    private static final double TOP_RANK_RATIO = 0.2;

    /** 后N%为负向信号的排名阈值 */
    private static final double BOTTOM_RANK_RATIO = 0.8;

    /** 超大单占比阈值（%），超过该值表示机构参与度高 */
    private static final double SUPER_LARGE_THRESHOLD = 50.0;

    /** 流入金额归一化基准（亿元） */
    private static final double INFLOW_NORMALIZE_BASE = 10_000_000_000.0;

    @Override
    public AgentType getAgentType() {
        return AgentType.SECTOR;
    }

    @Override
    public SectorHeat analyze(AgentContext context) {
        log.info("SectorAgent 开始分析资金面");

        SectorDataResult sectorResult = context.getSectorResult();
        if (sectorResult == null || sectorResult.getTodayFlow() == null) {
            log.warn("无板块数据，返回中性信号");
            return SectorHeat.empty("no_sector_data");
        }

        SectorMoneyFlow todayFlow = sectorResult.getTodayFlow();
        TrendIndicator trend = sectorResult.getTrendIndicator();

        // 1. 计算资金面信号
        SignalType capitalSignal = determineCapitalSignal(
            todayFlow.getMainNetInflow(),
            sectorResult.getInflowRank(),
            sectorResult.getTotalSectors()
        );

        // 2. 计算情绪面信号（基于超大单占比判断机构参与度）
        BigDecimal superLargeRatio = calculateSuperLargeRatio(todayFlow);
        SignalType sentimentSignal = superLargeRatio.doubleValue() > SUPER_LARGE_THRESHOLD
            ? SignalType.POSITIVE : SignalType.NEUTRAL;

        // 3. 计算热度评分（综合排名、流入金额、连续天数）
        int heatScore = calculateHeatScore(sectorResult);

        // 4. 构建资金结构
        CapitalStructure structure = buildCapitalStructure(todayFlow);

        // 5. 获取连续流入天数
        int consecutiveInflowDays = trend != null ? trend.getConsecutiveInflowDays() : 0;

        log.info("SectorAgent 分析完成，资金信号={}，情绪信号={}，热度={}",
                 capitalSignal, sentimentSignal, heatScore);

        return SectorHeat.builder()
            .capitalSignal(capitalSignal)
            .sentimentSignal(sentimentSignal)
            .mainNetInflow(todayFlow.getMainNetInflow())
            .consecutiveInflowDays(consecutiveInflowDays)
            .superLargeRatio(superLargeRatio)
            .inflowRank(sectorResult.getInflowRank())
            .totalSectors(sectorResult.getTotalSectors())
            .capitalStructure(structure)
            .heatScore(heatScore)
            .status(AnalysisStatus.normal())
            .build();
    }

    /**
     * 判定资金面信号
     */
    private SignalType determineCapitalSignal(BigDecimal inflow, int rank, long total) {
        if (inflow == null || total == 0) return SignalType.NEUTRAL;

        boolean isPositiveInflow = inflow.compareTo(BigDecimal.ZERO) > 0;
        boolean isTopRank = rank > 0 && rank <= total * TOP_RANK_RATIO;
        boolean isBottomRank = rank > 0 && rank > total * BOTTOM_RANK_RATIO;

        if (isPositiveInflow && isTopRank) return SignalType.POSITIVE;
        if (!isPositiveInflow && isBottomRank) return SignalType.NEGATIVE;
        return SignalType.NEUTRAL;
    }

    /**
     * 计算超大单占比
     * 计算方式：超大单净流入 / (超大单净流入绝对值 + 大单净流入绝对值) * 100
     */
    private BigDecimal calculateSuperLargeRatio(SectorMoneyFlow flow) {
        if (flow.getSuperLargeInflowRatio() != null) {
            return flow.getSuperLargeInflowRatio();
        }

        BigDecimal superLarge = flow.getSuperLargeInflow();
        BigDecimal large = flow.getLargeInflow();

        if (superLarge == null || large == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = superLarge.abs().add(large.abs());
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return superLarge.abs()
            .multiply(BigDecimal.valueOf(100))
            .divide(total, 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算热度评分
     * 加权评分：排名(40%) + 连续天数(30%) + 流入金额归一化(30%)
     */
    private int calculateHeatScore(SectorDataResult result) {
        // 排名评分（前20%得40分，线性递减）
        int rankScore = 0;
        if (result.getInflowRank() > 0 && result.getTotalSectors() > 0) {
            double rankRatio = (double) result.getInflowRank() / result.getTotalSectors();
            rankScore = (int) ((1 - rankRatio) * 40);
        }

        // 连续天数评分（每天10分，最高30分）
        int consecutiveScore = 0;
        if (result.getTrendIndicator() != null) {
            consecutiveScore = Math.min(30, result.getTrendIndicator().getConsecutiveInflowDays() * 10);
        }

        // 流入金额评分（以10亿为基准，最高30分）
        int inflowScore = normalizeInflowScore(result.getTodayFlow().getMainNetInflow());

        return Math.min(100, Math.max(0, rankScore + consecutiveScore + inflowScore));
    }

    /**
     * 将流入金额归一化为评分（0-30分）
     */
    private int normalizeInflowScore(BigDecimal inflow) {
        if (inflow == null) return 0;
        double value = inflow.doubleValue();
        if (value <= 0) return 0;
        // 以10亿为满分基准
        return (int) Math.min(30, (value / INFLOW_NORMALIZE_BASE) * 30);
    }

    /**
     * 构建资金结构详情
     */
    private CapitalStructure buildCapitalStructure(SectorMoneyFlow flow) {
        return CapitalStructure.builder()
            .superLargeInflow(flow.getSuperLargeInflow())
            .largeInflow(flow.getLargeInflow())
            .mediumInflow(flow.getMediumInflow())
            .smallInflow(flow.getSmallInflow())
            .build();
    }
}

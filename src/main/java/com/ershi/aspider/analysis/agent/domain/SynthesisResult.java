package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SynthesisAgent 输出结果 - 综合研判
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynthesisResult {

    /** 综合评级 */
    private OverallRating overallRating;

    /** 综合评分 0-100 */
    private int overallScore;

    /** 四维信号灯 */
    private DimensionSignals dimensionSignals;

    /** 跨维度关联分析（LLM生成的洞察） */
    private List<String> crossDimensionInsights;

    /** 一句话总结 */
    private String summary;

    /** 是否由LLM生成（false表示降级为规则合成） */
    private boolean llmGenerated;

    /** 分析状态 */
    private AnalysisStatus status;

    /**
     * 规则降级时的兜底合成
     */
    public static SynthesisResult fallback(PolicyImpact policy, SectorHeat sector, TrendSignal trend) {
        int positiveCount = countPositive(
            policy != null ? policy.getSignal() : SignalType.NEUTRAL,
            sector != null ? sector.getCapitalSignal() : SignalType.NEUTRAL,
            trend != null ? trend.getSignal() : SignalType.NEUTRAL
        );
        OverallRating rating = calculateRating(positiveCount);
        int score = calculateScore(positiveCount, sector);
        String summary = generateTemplateSummary(policy, sector, trend);

        DimensionSignals signals = DimensionSignals.builder()
            .news(policy != null ? policy.getSignal() : SignalType.NEUTRAL)
            .capital(sector != null ? sector.getCapitalSignal() : SignalType.NEUTRAL)
            .technical(trend != null ? trend.getSignal() : SignalType.NEUTRAL)
            .sentiment(sector != null ? sector.getSentimentSignal() : SignalType.NEUTRAL)
            .build();

        return SynthesisResult.builder()
            .overallRating(rating)
            .overallScore(score)
            .dimensionSignals(signals)
            .crossDimensionInsights(Collections.emptyList())
            .summary(summary)
            .llmGenerated(false)
            .status(AnalysisStatus.degraded("llm_fallback"))
            .build();
    }

    private static int countPositive(SignalType... signals) {
        int count = 0;
        for (SignalType s : signals) {
            if (s == SignalType.POSITIVE) count++;
        }
        return count;
    }

    private static OverallRating calculateRating(int positiveCount) {
        switch (positiveCount) {
            case 3:
                return OverallRating.STRONG_BULLISH;
            case 2:
                return OverallRating.BULLISH;
            case 1:
                return OverallRating.NEUTRAL;
            default:
                return OverallRating.BEARISH;
        }
    }

    private static int calculateScore(int positiveCount, SectorHeat sector) {
        int base = positiveCount * 25;
        if (sector != null && sector.getHeatScore() > 0) {
            base = (base + sector.getHeatScore()) / 2;
        }
        return Math.min(100, Math.max(0, base));
    }

    private static String generateTemplateSummary(PolicyImpact policy, SectorHeat sector, TrendSignal trend) {
        List<String> parts = new ArrayList<>();
        if (policy != null && policy.getSignal() == SignalType.POSITIVE) {
            parts.add("消息面偏多");
        }
        if (sector != null && sector.getCapitalSignal() == SignalType.POSITIVE) {
            parts.add("资金持续流入");
        }
        if (trend != null && trend.getSignal() == SignalType.POSITIVE) {
            parts.add("趋势向好");
        }
        if (parts.isEmpty()) {
            return "多空因素交织，需观望";
        }
        return String.join("，", parts);
    }
}

package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * PolicyAgent 输出结果 - 消息面分析
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyImpact {

    /** 消息面信号 */
    private SignalType signal;

    /** 核心驱动因素（从重要新闻标题提取） */
    private List<String> coreDrivers;

    /** 潜在风险因素 */
    private List<String> potentialRisks;

    /** 相关政策新闻（按重要性排序，最多5条） */
    private List<PolicyNewsItem> relatedNews;

    /** 置信度 0-1（基于证据数量） */
    private double confidence;

    /** 分析状态 */
    private AnalysisStatus status;

    /**
     * 创建空结果（降级时使用）
     */
    public static PolicyImpact empty(String reason) {
        return PolicyImpact.builder()
            .signal(SignalType.NEUTRAL)
            .coreDrivers(Collections.emptyList())
            .potentialRisks(Collections.emptyList())
            .relatedNews(Collections.emptyList())
            .confidence(0)
            .status(AnalysisStatus.degraded(reason))
            .build();
    }
}

package com.ershi.aspider.analysis.agent.domain;

import com.ershi.aspider.analysis.retriever.domain.enums.TrendDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * TrendAgent 输出结果 - 趋势研判
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendSignal {

    /** 趋势信号 */
    private SignalType signal;

    /** 趋势方向 */
    private TrendDirection direction;

    /** 短期研判（1-5日） */
    private TrendView shortTerm;

    /** 中期研判（1-4周） */
    private TrendView midTerm;

    /** 支撑位 */
    private BigDecimal supportLevel;

    /** 压力位 */
    private BigDecimal resistanceLevel;

    /** 风险提示列表 */
    private List<String> riskWarnings;

    /** 分析状态 */
    private AnalysisStatus status;

    /**
     * 创建空结果（降级时使用）
     */
    public static TrendSignal empty(String reason) {
        return TrendSignal.builder()
            .signal(SignalType.NEUTRAL)
            .direction(TrendDirection.NEUTRAL)
            .shortTerm(TrendView.builder().viewpoint("数据不足").basis("").build())
            .midTerm(TrendView.builder().viewpoint("数据不足").basis("").build())
            .supportLevel(null)
            .resistanceLevel(null)
            .riskWarnings(Collections.emptyList())
            .status(AnalysisStatus.degraded(reason))
            .build();
    }
}

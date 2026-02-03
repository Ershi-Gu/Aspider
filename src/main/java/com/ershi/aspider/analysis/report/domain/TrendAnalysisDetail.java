package com.ershi.aspider.analysis.report.domain;

import com.ershi.aspider.analysis.agent.domain.SignalType;
import com.ershi.aspider.analysis.agent.domain.TrendView;
import com.ershi.aspider.analysis.retriever.domain.enums.TrendDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 趋势研判详细分析
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysisDetail {

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
}

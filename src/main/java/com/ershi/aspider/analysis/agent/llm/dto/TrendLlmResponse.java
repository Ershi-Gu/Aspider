package com.ershi.aspider.analysis.agent.llm.dto;

import com.ershi.aspider.analysis.agent.domain.SignalType;
import com.ershi.aspider.analysis.agent.domain.TrendView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TrendAgent LLM响应DTO
 *
 * 只包含LLM生成的趋势观点和风险提示
 * direction/supportLevel/resistanceLevel 由系统填充
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendLlmResponse {

    /** 趋势信号：POSITIVE|NEGATIVE|NEUTRAL */
    private SignalType signal;

    /** 短期研判（1-5日） */
    private TrendView shortTerm;

    /** 中期研判（1-4周） */
    private TrendView midTerm;

    /** 风险提示（最多3条） */
    private List<String> riskWarnings;
}

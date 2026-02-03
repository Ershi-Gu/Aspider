package com.ershi.aspider.analysis.agent.llm.dto;

import com.ershi.aspider.analysis.agent.domain.SignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PolicyAgent LLM响应DTO
 *
 * 只包含LLM生成的非数值字段
 * confidence/relatedNews 由系统填充
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyLlmResponse {

    /** 消息面信号：POSITIVE|NEGATIVE|NEUTRAL */
    private SignalType signal;

    /** 核心驱动因素（最多3条） */
    private List<String> coreDrivers;

    /** 潜在风险因素（最多3条） */
    private List<String> potentialRisks;
}

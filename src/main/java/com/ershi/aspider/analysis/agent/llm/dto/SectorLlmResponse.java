package com.ershi.aspider.analysis.agent.llm.dto;

import com.ershi.aspider.analysis.agent.domain.SignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SectorAgent LLM响应DTO
 *
 * 只包含LLM生成的信号字段
 * 所有数值字段（mainNetInflow/heatScore/inflowRank等）由系统填充
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorLlmResponse {

    /** 资金面信号：POSITIVE|NEGATIVE|NEUTRAL */
    private SignalType capitalSignal;

    /** 情绪面信号：POSITIVE|NEUTRAL */
    private SignalType sentimentSignal;
}

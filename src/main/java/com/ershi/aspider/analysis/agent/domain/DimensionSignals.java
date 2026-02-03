package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 四维信号灯
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionSignals {

    /** 消息面 ← PolicyAgent */
    private SignalType news;

    /** 资金面 ← SectorAgent */
    private SignalType capital;

    /** 技术面 ← TrendAgent（趋势代替） */
    private SignalType technical;

    /** 情绪面 ← SectorAgent（超大单占比） */
    private SignalType sentiment;
}

package com.ershi.aspider.analysis.agent.core;

import com.ershi.aspider.analysis.agent.domain.PolicyImpact;
import com.ershi.aspider.analysis.agent.domain.SectorHeat;
import com.ershi.aspider.analysis.agent.domain.TrendSignal;
import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.RetrievalResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SynthesisAgent 专用输入
 * 包含三个子Agent的输出以及原始检索数据
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynthesisInput {

    /** 原始检索结果（用于补充上下文） */
    private RetrievalResult retrievalResult;

    /** PolicyAgent 输出 */
    private PolicyImpact policyImpact;

    /** SectorAgent 输出 */
    private SectorHeat sectorHeat;

    /** TrendAgent 输出 */
    private TrendSignal trendSignal;

    /** 分析查询参数 */
    private AnalysisQuery query;
}

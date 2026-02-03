package com.ershi.aspider.analysis.agent.core;

import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.NewsRetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 子Agent通用输入上下文
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    /** 原始检索结果 */
    private RetrievalResult retrievalResult;

    /** 分析查询参数 */
    private AnalysisQuery query;

    /** 分析时间 */
    private LocalDateTime analysisTime;

    /**
     * 便捷方法：获取新闻检索结果
     */
    public NewsRetrievalResult getNewsResult() {
        return retrievalResult != null ? retrievalResult.getNewsResult() : null;
    }

    /**
     * 便捷方法：获取板块数据结果
     */
    public SectorDataResult getSectorResult() {
        return retrievalResult != null ? retrievalResult.getSectorResult() : null;
    }
}

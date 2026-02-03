package com.ershi.aspider.analysis.report.domain;

import com.ershi.aspider.analysis.agent.domain.PolicyNewsItem;
import com.ershi.aspider.analysis.agent.domain.SignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息面详细分析
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsAnalysisDetail {

    /** 消息面信号 */
    private SignalType signal;

    /** 核心驱动因素 */
    private List<String> coreDrivers;

    /** 潜在风险因素 */
    private List<String> potentialRisks;

    /** 相关新闻列表 */
    private List<PolicyNewsItem> relatedNews;
}

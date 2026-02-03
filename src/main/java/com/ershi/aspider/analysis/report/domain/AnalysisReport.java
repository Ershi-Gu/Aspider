package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 完整分析报告（L1-L4）
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReport {

    /** L1 信号卡片（5秒决策） */
    private SignalCard signalCard;

    /** L2 数据看板（30秒了解） */
    private DataDashboard dashboard;

    /** L3 详细分析（3分钟深度） */
    private DetailAnalysis detailAnalysis;

    /** L4 操作参考 */
    private ActionReference actionReference;

    /** 报告生成时间 */
    private LocalDateTime generateTime;

    /** 数据时效性（交易日期） */
    private LocalDate tradeDate;
}

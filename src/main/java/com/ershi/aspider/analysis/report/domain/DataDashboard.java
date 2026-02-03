package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * L2 数据看板（30秒了解）
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDashboard {

    /** 今日表现 */
    private TodayPerformance todayPerformance;

    /** 资金动向 */
    private CapitalMovement capitalMovement;

    /** 近5日涨跌幅（按日期升序） */
    private List<DailyChange> recentChanges;

    /** 近5日资金流向（按日期升序） */
    private List<DailyInflow> recentInflows;
}

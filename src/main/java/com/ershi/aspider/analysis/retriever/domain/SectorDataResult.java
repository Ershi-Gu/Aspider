package com.ershi.aspider.analysis.retriever.domain;

import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import lombok.Data;

import java.util.List;

/**
 * 板块数据检索结果
 *
 * @author Ershi-Gu
 */
@Data
public class SectorDataResult {

    /** 当日资金流向 */
    private SectorMoneyFlow todayFlow;

    /** 近N日资金流向（按日期降序） */
    private List<SectorMoneyFlow> recentFlows;

    /** 当日行情 */
    private SectorQuote todayQuote;

    /** 资金流入排名（从1开始，-1表示未找到） */
    private int inflowRank;

    /** 板块总数 */
    private long totalSectors;

    /** 趋势指标 */
    private TrendIndicator trendIndicator;
}

package com.ershi.aspider.analysis.retriever;

import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.analysis.retriever.domain.enums.TrendDirection;
import com.ershi.aspider.analysis.retriever.domain.TrendIndicator;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import com.ershi.aspider.data.storage.elasticsearch.service.SectorMoneyFlowStorageService;
import com.ershi.aspider.data.storage.elasticsearch.service.SectorQuoteStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 板块数据检索器
 * <p>
 * 获取指定板块的资金流向、行情数据，并计算趋势指标。
 *
 * @author Ershi-Gu
 */
@Service
public class SectorDataRetriever implements DataRetriever<AnalysisQuery, SectorDataResult> {

    private static final Logger log = LoggerFactory.getLogger(SectorDataRetriever.class);

    private static final int DEFAULT_TREND_DAYS = 5;

    /** 趋势判定阈值 */
    private static final double STRONG_UP_THRESHOLD = 2.0;
    private static final double UP_THRESHOLD = 0.3;
    private static final double DOWN_THRESHOLD = -0.3;
    private static final double STRONG_DOWN_THRESHOLD = -2.0;

    private final SectorMoneyFlowStorageService moneyFlowStorage;
    private final SectorQuoteStorageService quoteStorage;

    public SectorDataRetriever(SectorMoneyFlowStorageService moneyFlowStorage,
                               SectorQuoteStorageService quoteStorage) {
        this.moneyFlowStorage = moneyFlowStorage;
        this.quoteStorage = quoteStorage;
    }

    @Override
    public SectorDataResult retrieve(AnalysisQuery query) {
        AnalysisQuery safeQuery = query != null ? query : new AnalysisQuery();
        LocalDate tradeDate = safeQuery.getTradeDate() != null ? safeQuery.getTradeDate() : LocalDate.now();
        int trendDays = safeQuery.getTrendDays() > 0 ? safeQuery.getTrendDays() : DEFAULT_TREND_DAYS;
        String sectorCode = safeQuery.getSectorCode();
        String sectorName = safeQuery.getSectorName();

        log.info("开始板块数据检索，日期={}，板块代码={}，板块名称={}", tradeDate, sectorCode, sectorName);

        // 获取当日资金流向
        SectorMoneyFlow todayFlow = resolveTodayFlow(sectorCode, sectorName, tradeDate);

        // 若通过名称找到了资金流向，提取板块代码用于后续查询
        String resolvedCode = sectorCode;
        if ((resolvedCode == null || resolvedCode.isBlank()) && todayFlow != null) {
            resolvedCode = todayFlow.getSectorCode();
        }

        // 获取近N日资金流向（用于趋势分析）
        List<SectorMoneyFlow> recentFlows = (resolvedCode == null || resolvedCode.isBlank())
            ? Collections.emptyList()
            : moneyFlowStorage.findRecentBySectorCode(resolvedCode, trendDays);

        // 获取当日行情
        SectorQuote todayQuote = resolveTodayQuote(resolvedCode, sectorName, tradeDate);

        // 计算排名
        int inflowRank = (resolvedCode == null || resolvedCode.isBlank())
            ? -1
            : moneyFlowStorage.calculateInflowRank(resolvedCode, tradeDate);
        long totalSectors = moneyFlowStorage.countByTradeDate(tradeDate);

        // 计算趋势指标
        TrendIndicator trendIndicator = calculateTrendFromFlows(recentFlows);

        log.info("板块数据检索完成，排名={}/{}, 连续流入天数={}", inflowRank, totalSectors,
                 trendIndicator.getConsecutiveInflowDays());

        SectorDataResult result = new SectorDataResult();
        result.setTodayFlow(todayFlow);
        result.setRecentFlows(recentFlows);
        result.setTodayQuote(todayQuote);
        result.setInflowRank(inflowRank);
        result.setTotalSectors(totalSectors);
        result.setTrendIndicator(trendIndicator);
        return result;
    }

    /**
     * 获取Top N板块
     *
     * @param date 交易日期
     * @param topN 数量
     * @param type 排序类型：INFLOW（主力净流入）/ CHANGE（涨跌幅）
     */
    public List<SectorMoneyFlow> getTopSectors(LocalDate date, int topN, TopSectorType type) {
        if (date == null || topN <= 0) {
            return Collections.emptyList();
        }
        TopSectorType resolvedType = type != null ? type : TopSectorType.INFLOW;
        if (resolvedType == TopSectorType.CHANGE) {
            return moneyFlowStorage.findTopByChangePercent(date, topN);
        }
        return moneyFlowStorage.findTopByMainNetInflow(date, topN);
    }

    /**
     * 计算趋势指标
     */
    public TrendIndicator calculateTrend(String sectorCode, int days) {
        if (sectorCode == null || sectorCode.isBlank()) {
            return emptyTrend();
        }
        int trendDays = days > 0 ? days : DEFAULT_TREND_DAYS;
        List<SectorMoneyFlow> recentFlows = moneyFlowStorage.findRecentBySectorCode(sectorCode, trendDays);
        return calculateTrendFromFlows(recentFlows);
    }

    /** 根据板块代码或名称获取当日资金流向 */
    private SectorMoneyFlow resolveTodayFlow(String sectorCode, String sectorName, LocalDate tradeDate) {
        // 优先通过板块名称查询
        if (sectorName != null && !sectorName.isBlank()) {
            SectorMoneyFlow flow = moneyFlowStorage.findBySectorNameAndDate(sectorName, tradeDate);
            if (flow != null) {
                return flow;
            }
        }
        // 其次通过板块代码查询
        if (sectorCode != null && !sectorCode.isBlank()) {
            List<SectorMoneyFlow> flows = moneyFlowStorage.findRecentBySectorCode(sectorCode, 1);
            if (!flows.isEmpty()) {
                SectorMoneyFlow flow = flows.get(0);
                if (tradeDate.equals(flow.getTradeDate())) {
                    return flow;
                }
            }
        }
        return null;
    }

    /** 根据板块代码或名称获取当日行情 */
    private SectorQuote resolveTodayQuote(String sectorCode, String sectorName, LocalDate tradeDate) {
        // 优先通过板块代码查询
        if (sectorCode != null && !sectorCode.isBlank()) {
            List<SectorQuote> quotes = quoteStorage.findBySectorCode(sectorCode, 5);
            for (SectorQuote quote : quotes) {
                if (quote != null && tradeDate.equals(quote.getTradeDate())) {
                    return quote;
                }
            }
            if (!quotes.isEmpty()) {
                return quotes.get(0);
            }
        }
        // 其次通过名称在当日数据中查找
        if (sectorName != null && !sectorName.isBlank()) {
            List<SectorQuote> quotes = quoteStorage.findByTradeDate(tradeDate);
            for (SectorQuote quote : quotes) {
                if (quote != null && sectorName.equals(quote.getSectorName())) {
                    return quote;
                }
            }
        }
        return null;
    }

    /** 根据近N日资金流向计算趋势指标 */
    private TrendIndicator calculateTrendFromFlows(List<SectorMoneyFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            return emptyTrend();
        }

        double totalInflow = 0;
        double totalChange = 0;
        int changeCount = 0;
        int consecutive = 0;
        boolean counting = true;

        for (SectorMoneyFlow flow : flows) {
            if (flow == null) {
                continue;
            }

            BigDecimal inflow = flow.getMainNetInflow();
            if (inflow != null) {
                totalInflow += inflow.doubleValue();
            }

            BigDecimal changePercent = flow.getChangePercent();
            if (changePercent != null) {
                totalChange += changePercent.doubleValue();
                changeCount++;
            }

            // 统计连续资金流入天数
            if (counting) {
                if (inflow != null && inflow.compareTo(BigDecimal.ZERO) > 0) {
                    consecutive++;
                } else {
                    counting = false;
                }
            }
        }

        double avgChange = changeCount > 0 ? totalChange / changeCount : 0;

        TrendIndicator indicator = new TrendIndicator();
        indicator.setConsecutiveInflowDays(consecutive);
        indicator.setTotalInflow(totalInflow);
        indicator.setAvgChangePercent(avgChange);
        indicator.setDirection(resolveDirection(avgChange, consecutive));
        return indicator;
    }

    private TrendIndicator emptyTrend() {
        TrendIndicator indicator = new TrendIndicator();
        indicator.setConsecutiveInflowDays(0);
        indicator.setTotalInflow(0);
        indicator.setAvgChangePercent(0);
        indicator.setDirection(TrendDirection.NEUTRAL);
        return indicator;
    }

    /** 根据平均涨跌幅和连续流入天数判定趋势方向 */
    private TrendDirection resolveDirection(double avgChangePercent, int consecutiveInflowDays) {
        if (avgChangePercent >= STRONG_UP_THRESHOLD || consecutiveInflowDays >= 3) {
            return TrendDirection.STRONG_UP;
        }
        if (avgChangePercent > UP_THRESHOLD) {
            return TrendDirection.UP;
        }
        if (avgChangePercent <= STRONG_DOWN_THRESHOLD) {
            return TrendDirection.STRONG_DOWN;
        }
        if (avgChangePercent < DOWN_THRESHOLD) {
            return TrendDirection.DOWN;
        }
        return TrendDirection.NEUTRAL;
    }

    /** Top板块排序类型 */
    public enum TopSectorType {
        INFLOW,
        CHANGE
    }
}

package com.ershi.aspider.analysis.report;

import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.report.domain.*;
import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.RetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 报告生成服务
 *
 * 纯结构组装，无业务逻辑
 * 将Agent分析结果组装为L1-L4报告结构
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class ReportGeneratorService {

    /**
     * 组装完整报告（L1-L4）
     *
     * @param query     分析查询参数
     * @param retrieval 检索结果
     * @param policy    消息面分析结果
     * @param sector    资金面分析结果
     * @param trend     趋势分析结果
     * @param synthesis 综合研判结果
     * @return 完整分析报告
     */
    public AnalysisReport generate(
            AnalysisQuery query,
            RetrievalResult retrieval,
            PolicyImpact policy,
            SectorHeat sector,
            TrendSignal trend,
            SynthesisResult synthesis) {

        log.info("开始组装分析报告，板块={}", query.getSectorName());

        // L1 信号卡片（5秒决策）
        SignalCard signalCard = buildSignalCard(query, synthesis);

        // L2 数据看板（30秒了解）
        DataDashboard dashboard = buildDashboard(retrieval.getSectorResult(), sector);

        // L3 详细分析（3分钟深度）
        DetailAnalysis detail = buildDetailAnalysis(policy, sector, trend);

        // L4 操作参考
        ActionReference action = buildActionReference(trend);

        AnalysisReport report = AnalysisReport.builder()
            .signalCard(signalCard)
            .dashboard(dashboard)
            .detailAnalysis(detail)
            .actionReference(action)
            .generateTime(LocalDateTime.now())
            .tradeDate(query.getTradeDate())
            .build();

        log.info("报告组装完成");
        return report;
    }

    /**
     * 构建L1信号卡片
     */
    private SignalCard buildSignalCard(AnalysisQuery query, SynthesisResult synthesis) {
        return SignalCard.builder()
            .sectorName(query.getSectorName())
            .tradeDate(query.getTradeDate())
            .overallRating(synthesis.getOverallRating())
            .overallScore(synthesis.getOverallScore())
            .dimensionSignals(synthesis.getDimensionSignals())
            .summary(synthesis.getSummary())
            .build();
    }

    /**
     * 构建L2数据看板
     */
    private DataDashboard buildDashboard(SectorDataResult sectorData, SectorHeat heat) {
        TodayPerformance todayPerformance = buildTodayPerformance(sectorData);
        CapitalMovement capitalMovement = buildCapitalMovement(heat);
        List<DailyChange> recentChanges = buildRecentChanges(sectorData);
        List<DailyInflow> recentInflows = buildRecentInflows(sectorData);

        return DataDashboard.builder()
            .todayPerformance(todayPerformance)
            .capitalMovement(capitalMovement)
            .recentChanges(recentChanges)
            .recentInflows(recentInflows)
            .build();
    }

    /**
     * 构建今日表现
     */
    private TodayPerformance buildTodayPerformance(SectorDataResult sectorData) {
        if (sectorData == null) {
            return TodayPerformance.builder().build();
        }

        SectorQuote quote = sectorData.getTodayQuote();
        SectorMoneyFlow flow = sectorData.getTodayFlow();

        return TodayPerformance.builder()
            .changePercent(quote != null ? quote.getChangePercent() : null)
            .rank(sectorData.getInflowRank())
            .totalSectors(sectorData.getTotalSectors())
            .leadStockName(flow != null ? flow.getLeadStockName() : null)
            .turnoverRate(quote != null ? quote.getTurnoverRate() : null)
            .build();
    }

    /**
     * 构建资金动向
     */
    private CapitalMovement buildCapitalMovement(SectorHeat heat) {
        if (heat == null) {
            return CapitalMovement.builder().build();
        }

        return CapitalMovement.builder()
            .mainNetInflow(heat.getMainNetInflow())
            .consecutiveInflowDays(heat.getConsecutiveInflowDays())
            .superLargeRatio(heat.getSuperLargeRatio())
            .build();
    }

    /**
     * 构建近N日涨跌幅
     */
    private List<DailyChange> buildRecentChanges(SectorDataResult sectorData) {
        if (sectorData == null || sectorData.getRecentFlows() == null) {
            return Collections.emptyList();
        }

        List<DailyChange> changes = new ArrayList<>();
        // 按日期升序（原始数据是降序）
        List<SectorMoneyFlow> flows = new ArrayList<>(sectorData.getRecentFlows());
        Collections.reverse(flows);

        for (SectorMoneyFlow flow : flows) {
            changes.add(DailyChange.builder()
                .tradeDate(flow.getTradeDate())
                .changePercent(flow.getChangePercent())
                .build());
        }
        return changes;
    }

    /**
     * 构建近N日资金流向
     */
    private List<DailyInflow> buildRecentInflows(SectorDataResult sectorData) {
        if (sectorData == null || sectorData.getRecentFlows() == null) {
            return Collections.emptyList();
        }

        List<DailyInflow> inflows = new ArrayList<>();
        // 按日期升序
        List<SectorMoneyFlow> flows = new ArrayList<>(sectorData.getRecentFlows());
        Collections.reverse(flows);

        for (SectorMoneyFlow flow : flows) {
            inflows.add(DailyInflow.builder()
                .tradeDate(flow.getTradeDate())
                .mainNetInflow(flow.getMainNetInflow())
                .build());
        }
        return inflows;
    }

    /**
     * 构建L3详细分析
     */
    private DetailAnalysis buildDetailAnalysis(PolicyImpact policy, SectorHeat sector, TrendSignal trend) {
        NewsAnalysisDetail newsDetail = buildNewsDetail(policy);
        CapitalAnalysisDetail capitalDetail = buildCapitalDetail(sector);
        TrendAnalysisDetail trendDetail = buildTrendDetail(trend);

        return DetailAnalysis.builder()
            .newsAnalysis(newsDetail)
            .capitalAnalysis(capitalDetail)
            .trendAnalysis(trendDetail)
            .build();
    }

    /**
     * 构建消息面详细分析
     */
    private NewsAnalysisDetail buildNewsDetail(PolicyImpact policy) {
        if (policy == null) {
            return NewsAnalysisDetail.builder()
                .signal(SignalType.NEUTRAL)
                .coreDrivers(Collections.emptyList())
                .potentialRisks(Collections.emptyList())
                .relatedNews(Collections.emptyList())
                .build();
        }

        return NewsAnalysisDetail.builder()
            .signal(policy.getSignal())
            .coreDrivers(policy.getCoreDrivers())
            .potentialRisks(policy.getPotentialRisks())
            .relatedNews(policy.getRelatedNews())
            .build();
    }

    /**
     * 构建资金面详细分析
     */
    private CapitalAnalysisDetail buildCapitalDetail(SectorHeat sector) {
        if (sector == null) {
            return CapitalAnalysisDetail.builder()
                .capitalSignal(SignalType.NEUTRAL)
                .sentimentSignal(SignalType.NEUTRAL)
                .build();
        }

        return CapitalAnalysisDetail.builder()
            .capitalSignal(sector.getCapitalSignal())
            .sentimentSignal(sector.getSentimentSignal())
            .mainNetInflow(sector.getMainNetInflow())
            .consecutiveInflowDays(sector.getConsecutiveInflowDays())
            .superLargeRatio(sector.getSuperLargeRatio())
            .inflowRank(sector.getInflowRank())
            .totalSectors(sector.getTotalSectors())
            .capitalStructure(sector.getCapitalStructure())
            .heatScore(sector.getHeatScore())
            .build();
    }

    /**
     * 构建趋势研判详细分析
     */
    private TrendAnalysisDetail buildTrendDetail(TrendSignal trend) {
        if (trend == null) {
            return TrendAnalysisDetail.builder()
                .signal(SignalType.NEUTRAL)
                .riskWarnings(Collections.emptyList())
                .build();
        }

        return TrendAnalysisDetail.builder()
            .signal(trend.getSignal())
            .direction(trend.getDirection())
            .shortTerm(trend.getShortTerm())
            .midTerm(trend.getMidTerm())
            .supportLevel(trend.getSupportLevel())
            .resistanceLevel(trend.getResistanceLevel())
            .riskWarnings(trend.getRiskWarnings())
            .build();
    }

    /**
     * 构建L4操作参考
     */
    private ActionReference buildActionReference(TrendSignal trend) {
        if (trend == null) {
            return ActionReference.builder()
                .riskWarnings(Collections.singletonList("数据不足，建议观望"))
                .build();
        }

        return ActionReference.builder()
            .supportLevel(trend.getSupportLevel())
            .resistanceLevel(trend.getResistanceLevel())
            .riskWarnings(trend.getRiskWarnings())
            .build();
    }
}

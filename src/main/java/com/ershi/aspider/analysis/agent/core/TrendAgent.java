package com.ershi.aspider.analysis.agent.core;

import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.analysis.retriever.domain.TrendIndicator;
import com.ershi.aspider.analysis.retriever.domain.enums.TrendDirection;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 趋势研判Agent（规则驱动，不调用LLM）
 *
 * 从板块行情和资金流向数据中提取趋势信号、支撑压力位和风险提示
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class TrendAgent implements Agent<AgentContext, TrendSignal> {

    /** 连续流入/流出判定趋势的天数阈值 */
    private static final int TREND_DAYS_THRESHOLD = 3;

    /** 涨跌幅波动阈值（%） */
    private static final double VOLATILITY_THRESHOLD = 3.0;

    @Override
    public AgentType getAgentType() {
        return AgentType.TREND;
    }

    @Override
    public TrendSignal analyze(AgentContext context) {
        log.info("TrendAgent 开始分析趋势");

        SectorDataResult sectorResult = context.getSectorResult();
        if (sectorResult == null || sectorResult.getTrendIndicator() == null) {
            log.warn("无趋势数据，返回中性信号");
            return TrendSignal.empty("no_trend_data");
        }

        TrendIndicator indicator = sectorResult.getTrendIndicator();
        List<SectorMoneyFlow> recentFlows = sectorResult.getRecentFlows();
        SectorQuote todayQuote = sectorResult.getTodayQuote();

        // 1. 信号判定（基于TrendDirection）
        SignalType signal = mapDirectionToSignal(indicator.getDirection());

        // 2. 短期研判（1-5日）
        TrendView shortTerm = generateShortTermView(indicator, recentFlows);

        // 3. 中期研判（1-4周）
        TrendView midTerm = generateMidTermView(indicator);

        // 4. 计算支撑/压力位（近N日最低/最高价）
        BigDecimal support = calculateSupportLevel(recentFlows, todayQuote);
        BigDecimal resistance = calculateResistanceLevel(recentFlows, todayQuote);

        // 5. 风险提示
        List<String> risks = generateRiskWarnings(indicator, sectorResult);

        log.info("TrendAgent 分析完成，信号={}，方向={}", signal, indicator.getDirection());

        return TrendSignal.builder()
            .signal(signal)
            .direction(indicator.getDirection())
            .shortTerm(shortTerm)
            .midTerm(midTerm)
            .supportLevel(support)
            .resistanceLevel(resistance)
            .riskWarnings(risks)
            .status(AnalysisStatus.normal())
            .build();
    }

    /**
     * 将趋势方向映射为信号类型
     */
    private SignalType mapDirectionToSignal(TrendDirection direction) {
        if (direction == null) return SignalType.NEUTRAL;
        switch (direction) {
            case STRONG_UP:
            case UP:
                return SignalType.POSITIVE;
            case STRONG_DOWN:
            case DOWN:
                return SignalType.NEGATIVE;
            case NEUTRAL:
            default:
                return SignalType.NEUTRAL;
        }
    }

    /**
     * 生成短期研判（1-5日）
     */
    private TrendView generateShortTermView(TrendIndicator indicator, List<SectorMoneyFlow> flows) {
        String viewpoint;
        String basis;
        TrendDirection direction = indicator.getDirection();

        if (direction == TrendDirection.STRONG_UP) {
            viewpoint = "震荡偏强";
            basis = String.format("资金连续%d日流入，累计流入%.2f亿",
                indicator.getConsecutiveInflowDays(),
                indicator.getTotalInflow() / 100_000_000);
        } else if (direction == TrendDirection.UP) {
            viewpoint = "温和上行";
            basis = String.format("近期涨跌幅均值%.2f%%，资金面偏积极", indicator.getAvgChangePercent());
        } else if (direction == TrendDirection.DOWN) {
            viewpoint = "承压调整";
            basis = "资金流出迹象明显，短期宜谨慎";
        } else if (direction == TrendDirection.STRONG_DOWN) {
            viewpoint = "弱势下行";
            basis = String.format("资金连续流出，累计流出%.2f亿",
                Math.abs(indicator.getTotalInflow()) / 100_000_000);
        } else {
            viewpoint = "震荡整理";
            basis = "多空平衡，等待方向选择";
        }

        return TrendView.builder().viewpoint(viewpoint).basis(basis).build();
    }

    /**
     * 生成中期研判（1-4周）
     */
    private TrendView generateMidTermView(TrendIndicator indicator) {
        String viewpoint;
        String basis;

        double avgChange = indicator.getAvgChangePercent();
        int consecutiveDays = indicator.getConsecutiveInflowDays();

        if (consecutiveDays >= 5 && avgChange > 1) {
            viewpoint = "谨慎乐观";
            basis = "资金持续流入叠加涨势，中期趋势向好";
        } else if (consecutiveDays >= 3 && avgChange > 0) {
            viewpoint = "震荡偏强";
            basis = "资金流入态势延续，关注能否突破";
        } else if (consecutiveDays <= -3 || avgChange < -2) {
            viewpoint = "谨慎观望";
            basis = "资金流出或跌幅明显，等待企稳信号";
        } else {
            viewpoint = "中性震荡";
            basis = "缺乏明确方向，建议观望为主";
        }

        return TrendView.builder().viewpoint(viewpoint).basis(basis).build();
    }

    /**
     * 计算支撑位（近N日最低价或最低涨跌幅对应价位）
     */
    private BigDecimal calculateSupportLevel(List<SectorMoneyFlow> flows, SectorQuote quote) {
        if (quote != null && quote.getLowPrice() != null) {
            return quote.getLowPrice();
        }
        // 无行情数据时返回null
        return null;
    }

    /**
     * 计算压力位（近N日最高价）
     */
    private BigDecimal calculateResistanceLevel(List<SectorMoneyFlow> flows, SectorQuote quote) {
        if (quote != null && quote.getHighPrice() != null) {
            return quote.getHighPrice();
        }
        return null;
    }

    /**
     * 生成风险提示
     */
    private List<String> generateRiskWarnings(TrendIndicator indicator, SectorDataResult result) {
        List<String> warnings = new ArrayList<>();

        // 1. 高位风险
        if (indicator.getAvgChangePercent() > VOLATILITY_THRESHOLD) {
            warnings.add("近期涨幅较大，注意高位回调风险");
        }

        // 2. 资金流出风险
        if (indicator.getConsecutiveInflowDays() < 0 && Math.abs(indicator.getConsecutiveInflowDays()) >= 3) {
            warnings.add("资金持续流出，关注趋势反转风险");
        }

        // 3. 波动风险
        if (result.getTodayQuote() != null) {
            SectorQuote quote = result.getTodayQuote();
            if (quote.getAmplitude() != null && quote.getAmplitude().doubleValue() > 5) {
                warnings.add("当日振幅较大，短期波动加剧");
            }
        }

        // 4. 排名下滑风险
        if (result.getInflowRank() > 0 && result.getTotalSectors() > 0) {
            double rankRatio = (double) result.getInflowRank() / result.getTotalSectors();
            if (rankRatio > 0.7) {
                warnings.add("资金流入排名靠后，板块热度下降");
            }
        }

        return warnings.isEmpty() ? Collections.singletonList("暂无明显风险信号") : warnings;
    }
}

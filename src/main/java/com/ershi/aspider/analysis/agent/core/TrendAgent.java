package com.ershi.aspider.analysis.agent.core;

import com.alibaba.fastjson2.JSON;
import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.agent.llm.LlmAnalysisExecutor;
import com.ershi.aspider.analysis.agent.llm.LlmExecutionException;
import com.ershi.aspider.analysis.agent.llm.LlmParseException;
import com.ershi.aspider.analysis.agent.llm.dto.TrendLlmResponse;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptRenderException;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptTemplateNotFoundException;
import com.ershi.aspider.analysis.agent.rule.TrendRuleEngine;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.analysis.retriever.domain.TrendIndicator;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 趋势研判Agent（LLM驱动 + 规则降级）
 *
 * 从板块行情和资金流向数据中提取趋势信号、短期/中期研判和风险提示
 * LLM输出观点，数值字段（支撑位/压力位）由系统填充
 * LLM调用失败时自动降级为规则分析
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class TrendAgent implements Agent<AgentContext, TrendSignal> {

    private final LlmAnalysisExecutor llmExecutor;
    private final TrendRuleEngine ruleEngine;

    public TrendAgent(LlmAnalysisExecutor llmExecutor, TrendRuleEngine ruleEngine) {
        this.llmExecutor = llmExecutor;
        this.ruleEngine = ruleEngine;
    }

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

        // 尝试LLM分析
        try {
            TrendSignal result = analyzeLlm(context);
            log.info("TrendAgent LLM分析成功，信号={}，方向={}",
                     result.getSignal(), result.getDirection());
            return result;
        } catch (PromptTemplateNotFoundException e) {
            log.warn("TrendAgent Prompt模板缺失，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_PROMPT_MISSING);
        } catch (PromptRenderException e) {
            log.warn("TrendAgent Prompt渲染失败，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_LLM_FALLBACK);
        } catch (LlmExecutionException e) {
            log.warn("TrendAgent LLM执行失败，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_NON_AI);
        } catch (LlmParseException e) {
            log.warn("TrendAgent LLM响应解析失败，降级为规则分析，原始响应: {}", e.getRawResponse(), e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_LLM_PARSE_ERROR);
        } catch (Exception e) {
            log.error("TrendAgent 未知异常，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_NON_AI);
        }
    }

    /**
     * LLM驱动分析
     */
    private TrendSignal analyzeLlm(AgentContext context) {
        // 1. 构建Prompt变量
        Map<String, Object> variables = buildPromptVariables(context);

        // 2. 调用LLM
        TrendLlmResponse llmResponse = llmExecutor.execute(
            AgentType.TREND, variables, TrendLlmResponse.class);

        // 3. 合并系统填充字段（数值保护）
        return mergeWithSystemData(llmResponse, context);
    }

    /**
     * 构建Prompt变量
     */
    private Map<String, Object> buildPromptVariables(AgentContext context) {
        String sectorName = context.getQuery() != null ? context.getQuery().getSectorName() : "未知板块";
        String analysisTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        SectorDataResult sectorResult = context.getSectorResult();
        TrendIndicator indicator = sectorResult.getTrendIndicator();
        List<SectorMoneyFlow> recentFlows = sectorResult.getRecentFlows();
        SectorQuote todayQuote = sectorResult.getTodayQuote();

        // 构建趋势指标JSON
        Map<String, Object> trendIndicator = new HashMap<>();
        trendIndicator.put("direction", indicator.getDirection().name());
        trendIndicator.put("consecutiveInflowDays", indicator.getConsecutiveInflowDays());
        trendIndicator.put("totalInflow", formatInflow(indicator.getTotalInflow()));
        trendIndicator.put("avgChangePercent", String.format("%.2f%%", indicator.getAvgChangePercent()));

        // 构建近期资金流向JSON
        List<Map<String, Object>> recentFlowList = recentFlows != null ? recentFlows.stream()
            .limit(5)
            .map(flow -> {
                Map<String, Object> item = new HashMap<>();
                item.put("tradeDate", flow.getTradeDate() != null ? flow.getTradeDate().toString() : "");
                item.put("mainNetInflow", formatInflow(flow.getMainNetInflow()));
                item.put("changePercent", flow.getChangePercent() != null
                    ? String.format("%.2f%%", flow.getChangePercent()) : "0%");
                return item;
            })
            .collect(Collectors.toList()) : Collections.emptyList();

        // 构建当日行情JSON
        Map<String, Object> quoteInfo = new HashMap<>();
        if (todayQuote != null) {
            quoteInfo.put("openPrice", todayQuote.getOpenPrice());
            quoteInfo.put("closePrice", todayQuote.getClosePrice());
            quoteInfo.put("highPrice", todayQuote.getHighPrice());
            quoteInfo.put("lowPrice", todayQuote.getLowPrice());
            quoteInfo.put("changePercent", todayQuote.getChangePercent() != null
                ? String.format("%.2f%%", todayQuote.getChangePercent()) : "0%");
            quoteInfo.put("turnoverRate", todayQuote.getTurnoverRate() != null
                ? String.format("%.2f%%", todayQuote.getTurnoverRate()) : "0%");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("sector_name", sectorName);
        variables.put("analysis_time", analysisTime);
        variables.put("trend_indicator_json", JSON.toJSONString(trendIndicator));
        variables.put("recent_flow_json", JSON.toJSONString(recentFlowList));
        variables.put("today_quote_json", JSON.toJSONString(quoteInfo));
        return variables;
    }

    /**
     * 合并LLM输出与系统填充字段（数值保护）
     */
    private TrendSignal mergeWithSystemData(TrendLlmResponse llmResponse, AgentContext context) {
        SectorDataResult sectorResult = context.getSectorResult();
        TrendIndicator indicator = sectorResult.getTrendIndicator();
        SectorQuote todayQuote = sectorResult.getTodayQuote();

        // 数值字段从原始数据填充
        BigDecimal support = todayQuote != null ? todayQuote.getLowPrice() : null;
        BigDecimal resistance = todayQuote != null ? todayQuote.getHighPrice() : null;

        return TrendSignal.builder()
            .signal(llmResponse.getSignal() != null ? llmResponse.getSignal() : SignalType.NEUTRAL)
            .direction(indicator.getDirection())
            .shortTerm(llmResponse.getShortTerm() != null ? llmResponse.getShortTerm()
                : TrendView.builder().viewpoint("数据不足").basis("").build())
            .midTerm(llmResponse.getMidTerm() != null ? llmResponse.getMidTerm()
                : TrendView.builder().viewpoint("数据不足").basis("").build())
            .supportLevel(support)
            .resistanceLevel(resistance)
            .riskWarnings(llmResponse.getRiskWarnings() != null ? llmResponse.getRiskWarnings() : Collections.emptyList())
            .status(AnalysisStatus.normal())
            .build();
    }

    /**
     * 降级为规则分析
     */
    private TrendSignal analyzeWithDegradation(AgentContext context, String degradeMessage) {
        TrendSignal result = ruleEngine.analyze(context);
        result.setStatus(AnalysisStatus.degraded(degradeMessage));
        log.info("TrendAgent 规则分析完成（降级），信号={}，方向={}",
                 result.getSignal(), result.getDirection());
        return result;
    }

    /**
     * 格式化流入金额（亿元）
     */
    private String formatInflow(double inflow) {
        return String.format("%.2f亿", inflow / 100_000_000);
    }

    private String formatInflow(BigDecimal inflow) {
        if (inflow == null) return "0";
        return String.format("%.2f亿", inflow.doubleValue() / 100_000_000);
    }
}

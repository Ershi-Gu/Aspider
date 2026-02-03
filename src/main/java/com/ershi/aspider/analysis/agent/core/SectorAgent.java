package com.ershi.aspider.analysis.agent.core;

import com.alibaba.fastjson2.JSON;
import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.agent.llm.LlmAnalysisExecutor;
import com.ershi.aspider.analysis.agent.llm.LlmExecutionException;
import com.ershi.aspider.analysis.agent.llm.LlmParseException;
import com.ershi.aspider.analysis.agent.llm.dto.SectorLlmResponse;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptRenderException;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptTemplateNotFoundException;
import com.ershi.aspider.analysis.agent.rule.SectorRuleEngine;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.analysis.retriever.domain.TrendIndicator;
import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 资金面分析Agent（LLM驱动 + 规则降级）
 *
 * 从板块资金流向数据中提取资金面信号、情绪面信号
 * LLM只输出信号，数值字段由系统从原始数据填充
 * LLM调用失败时自动降级为规则分析
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class SectorAgent implements Agent<AgentContext, SectorHeat> {

    /** 超大单占比阈值（%），超过该值表示机构参与度高 */
    private static final double SUPER_LARGE_THRESHOLD = 50.0;

    /** 流入金额归一化基准（亿元） */
    private static final double INFLOW_NORMALIZE_BASE = 10_000_000_000.0;

    private final LlmAnalysisExecutor llmExecutor;
    private final SectorRuleEngine ruleEngine;

    public SectorAgent(LlmAnalysisExecutor llmExecutor, SectorRuleEngine ruleEngine) {
        this.llmExecutor = llmExecutor;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.SECTOR;
    }

    @Override
    public SectorHeat analyze(AgentContext context) {
        log.info("SectorAgent 开始分析资金面");

        SectorDataResult sectorResult = context.getSectorResult();
        if (sectorResult == null || sectorResult.getTodayFlow() == null) {
            log.warn("无板块数据，返回中性信号");
            return SectorHeat.empty("no_sector_data");
        }

        // 尝试LLM分析
        try {
            SectorHeat result = analyzeLlm(context);
            log.info("SectorAgent LLM分析成功，资金信号={}，情绪信号={}",
                     result.getCapitalSignal(), result.getSentimentSignal());
            return result;
        } catch (PromptTemplateNotFoundException e) {
            log.warn("SectorAgent Prompt模板缺失，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_PROMPT_MISSING);
        } catch (PromptRenderException e) {
            log.warn("SectorAgent Prompt渲染失败，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_LLM_FALLBACK);
        } catch (LlmExecutionException e) {
            log.warn("SectorAgent LLM执行失败，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_NON_AI);
        } catch (LlmParseException e) {
            log.warn("SectorAgent LLM响应解析失败，降级为规则分析，原始响应: {}", e.getRawResponse(), e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_LLM_PARSE_ERROR);
        } catch (Exception e) {
            log.error("SectorAgent 未知异常，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_NON_AI);
        }
    }

    /**
     * LLM驱动分析
     */
    private SectorHeat analyzeLlm(AgentContext context) {
        // 1. 构建Prompt变量
        Map<String, Object> variables = buildPromptVariables(context);

        // 2. 调用LLM
        SectorLlmResponse llmResponse = llmExecutor.execute(
            AgentType.SECTOR, variables, SectorLlmResponse.class);

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
        SectorMoneyFlow todayFlow = sectorResult.getTodayFlow();
        TrendIndicator trend = sectorResult.getTrendIndicator();

        // 构建资金指标JSON
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("mainNetInflow", formatInflow(todayFlow.getMainNetInflow()));
        metrics.put("inflowRank", sectorResult.getInflowRank());
        metrics.put("totalSectors", sectorResult.getTotalSectors());
        metrics.put("superLargeRatio", calculateSuperLargeRatio(todayFlow));
        metrics.put("consecutiveInflowDays", trend != null ? trend.getConsecutiveInflowDays() : 0);

        Map<String, Object> variables = new HashMap<>();
        variables.put("sector_name", sectorName);
        variables.put("analysis_time", analysisTime);
        variables.put("sector_metrics_json", JSON.toJSONString(metrics));
        return variables;
    }

    /**
     * 合并LLM输出与系统填充字段（数值保护）
     */
    private SectorHeat mergeWithSystemData(SectorLlmResponse llmResponse, AgentContext context) {
        SectorDataResult sectorResult = context.getSectorResult();
        SectorMoneyFlow todayFlow = sectorResult.getTodayFlow();
        TrendIndicator trend = sectorResult.getTrendIndicator();

        // 数值字段全部从原始数据填充
        BigDecimal superLargeRatio = calculateSuperLargeRatio(todayFlow);
        int heatScore = calculateHeatScore(sectorResult);
        int consecutiveInflowDays = trend != null ? trend.getConsecutiveInflowDays() : 0;

        CapitalStructure structure = CapitalStructure.builder()
            .superLargeInflow(todayFlow.getSuperLargeInflow())
            .largeInflow(todayFlow.getLargeInflow())
            .mediumInflow(todayFlow.getMediumInflow())
            .smallInflow(todayFlow.getSmallInflow())
            .build();

        return SectorHeat.builder()
            .capitalSignal(llmResponse.getCapitalSignal() != null ? llmResponse.getCapitalSignal() : SignalType.NEUTRAL)
            .sentimentSignal(llmResponse.getSentimentSignal() != null ? llmResponse.getSentimentSignal() : SignalType.NEUTRAL)
            .mainNetInflow(todayFlow.getMainNetInflow())
            .consecutiveInflowDays(consecutiveInflowDays)
            .superLargeRatio(superLargeRatio)
            .inflowRank(sectorResult.getInflowRank())
            .totalSectors(sectorResult.getTotalSectors())
            .capitalStructure(structure)
            .heatScore(heatScore)
            .status(AnalysisStatus.normal())
            .build();
    }

    /**
     * 降级为规则分析
     */
    private SectorHeat analyzeWithDegradation(AgentContext context, String degradeMessage) {
        SectorHeat result = ruleEngine.analyze(context);
        result.setStatus(AnalysisStatus.degraded(degradeMessage));
        log.info("SectorAgent 规则分析完成（降级），资金信号={}，情绪信号={}",
                 result.getCapitalSignal(), result.getSentimentSignal());
        return result;
    }

    /**
     * 格式化流入金额（亿元）
     */
    private String formatInflow(BigDecimal inflow) {
        if (inflow == null) return "0";
        return String.format("%.2f亿", inflow.doubleValue() / 100_000_000);
    }

    /**
     * 计算超大单占比
     */
    private BigDecimal calculateSuperLargeRatio(SectorMoneyFlow flow) {
        if (flow.getSuperLargeInflowRatio() != null) {
            return flow.getSuperLargeInflowRatio();
        }

        BigDecimal superLarge = flow.getSuperLargeInflow();
        BigDecimal large = flow.getLargeInflow();

        if (superLarge == null || large == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = superLarge.abs().add(large.abs());
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return superLarge.abs()
            .multiply(BigDecimal.valueOf(100))
            .divide(total, 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算热度评分
     */
    private int calculateHeatScore(SectorDataResult result) {
        int rankScore = 0;
        if (result.getInflowRank() > 0 && result.getTotalSectors() > 0) {
            double rankRatio = (double) result.getInflowRank() / result.getTotalSectors();
            rankScore = (int) ((1 - rankRatio) * 40);
        }

        int consecutiveScore = 0;
        if (result.getTrendIndicator() != null) {
            consecutiveScore = Math.min(30, result.getTrendIndicator().getConsecutiveInflowDays() * 10);
        }

        int inflowScore = normalizeInflowScore(result.getTodayFlow().getMainNetInflow());

        return Math.min(100, Math.max(0, rankScore + consecutiveScore + inflowScore));
    }

    /**
     * 将流入金额归一化为评分（0-30分）
     */
    private int normalizeInflowScore(BigDecimal inflow) {
        if (inflow == null) {
            return 0;
        }
        double value = inflow.doubleValue();
        if (value <= 0) {
            return 0;
        }
        return (int) Math.min(30, (value / INFLOW_NORMALIZE_BASE) * 30);
    }
}

package com.ershi.aspider.analysis.agent.core;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ershi.aspider.analysis.agent.config.AgentLlmClientFactory;
import com.ershi.aspider.analysis.agent.config.AgentLlmProperties;
import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptRenderer;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptTemplateRepository;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ResponseFormatJsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合研判Agent（LLM驱动）
 *
 * 接收三个子Agent的分析结果，进行跨维度关联推理，生成综合研判
 * Prompt模板从资源文件加载
 * 当LLM调用失败时，降级为规则合成
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class SynthesisAgent implements Agent<SynthesisInput, SynthesisResult> {

    private final AgentLlmClientFactory clientFactory;
    private final PromptTemplateRepository templateRepository;
    private final PromptRenderer promptRenderer;

    public SynthesisAgent(AgentLlmClientFactory clientFactory,
                          PromptTemplateRepository templateRepository,
                          PromptRenderer promptRenderer) {
        this.clientFactory = clientFactory;
        this.templateRepository = templateRepository;
        this.promptRenderer = promptRenderer;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.SYNTHESIS;
    }

    @Override
    public SynthesisResult analyze(SynthesisInput input) {
        log.info("SynthesisAgent 开始综合研判");

        // 1. 构建Prompt
        String prompt = buildPrompt(input);

        // 2. 调用LLM
        try {
            OpenAIClient client = clientFactory.getClient(AgentType.SYNTHESIS);
            AgentLlmProperties.LlmConfig config = clientFactory.getConfig(AgentType.SYNTHESIS);

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(config.getModel())
                .addUserMessage(prompt)
                .responseFormat(ResponseFormatJsonObject.builder().build())
                .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String content = completion.choices().get(0).message().content().orElse("");

            // 3. 解析JSON响应
            SynthesisResult result = parseResponse(content, input);
            result.setLlmGenerated(true);
            result.setStatus(AnalysisStatus.normal());

            log.info("SynthesisAgent LLM分析完成，评级={}", result.getOverallRating());
            return result;

        } catch (Exception e) {
            log.error("SynthesisAgent LLM调用失败，降级为规则合成", e);
            return SynthesisResult.fallback(
                input.getPolicyImpact(),
                input.getSectorHeat(),
                input.getTrendSignal()
            );
        }
    }

    /**
     * 构建LLM Prompt（从模板文件加载并渲染）
     */
    private String buildPrompt(SynthesisInput input) {
        String sectorName = input.getQuery() != null ? input.getQuery().getSectorName() : "未知板块";
        PolicyImpact policy = input.getPolicyImpact();
        SectorHeat sector = input.getSectorHeat();
        TrendSignal trend = input.getTrendSignal();

        // 构建模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("sector_name", sectorName);

        // 消息面变量
        variables.put("policy_signal", policy != null ? policy.getSignal().name() : "NEUTRAL");
        variables.put("core_drivers", policy != null && policy.getCoreDrivers() != null
            ? String.join("；", policy.getCoreDrivers()) : "无");
        variables.put("potential_risks", policy != null && policy.getPotentialRisks() != null
            ? String.join("；", policy.getPotentialRisks()) : "无");

        // 资金面变量
        variables.put("capital_signal", sector != null ? sector.getCapitalSignal().name() : "NEUTRAL");
        double mainNetInflow = sector != null && sector.getMainNetInflow() != null
            ? sector.getMainNetInflow().doubleValue() / 100_000_000 : 0;
        variables.put("main_net_inflow", String.format("%.2f", mainNetInflow));
        variables.put("consecutive_inflow_days", String.valueOf(sector != null ? sector.getConsecutiveInflowDays() : 0));
        variables.put("heat_score", String.valueOf(sector != null ? sector.getHeatScore() : 0));

        // 趋势变量
        variables.put("trend_signal", trend != null ? trend.getSignal().name() : "NEUTRAL");
        variables.put("short_term_view", trend != null && trend.getShortTerm() != null
            ? trend.getShortTerm().getViewpoint() : "数据不足");
        variables.put("mid_term_view", trend != null && trend.getMidTerm() != null
            ? trend.getMidTerm().getViewpoint() : "数据不足");

        // 加载模板并渲染
        try {
            String template = templateRepository.getTemplate(AgentType.SYNTHESIS);
            return promptRenderer.render(template, variables);
        } catch (Exception e) {
            log.warn("Prompt模板加载失败，使用内置模板", e);
            return buildFallbackPrompt(input, sectorName);
        }
    }

    /**
     * 内置备用Prompt（模板加载失败时使用）
     */
    private String buildFallbackPrompt(SynthesisInput input, String sectorName) {
        PolicyImpact policy = input.getPolicyImpact();
        SectorHeat sector = input.getSectorHeat();
        TrendSignal trend = input.getTrendSignal();

        String policySignal = policy != null ? policy.getSignal().name() : "NEUTRAL";
        String coreDrivers = policy != null && policy.getCoreDrivers() != null
            ? String.join("；", policy.getCoreDrivers()) : "无";
        String potentialRisks = policy != null && policy.getPotentialRisks() != null
            ? String.join("；", policy.getPotentialRisks()) : "无";

        String capitalSignal = sector != null ? sector.getCapitalSignal().name() : "NEUTRAL";
        double mainNetInflow = sector != null && sector.getMainNetInflow() != null
            ? sector.getMainNetInflow().doubleValue() / 100_000_000 : 0;
        int consecutiveInflowDays = sector != null ? sector.getConsecutiveInflowDays() : 0;
        int heatScore = sector != null ? sector.getHeatScore() : 0;

        String trendSignal = trend != null ? trend.getSignal().name() : "NEUTRAL";
        String shortTermView = trend != null && trend.getShortTerm() != null
            ? trend.getShortTerm().getViewpoint() : "数据不足";
        String midTermView = trend != null && trend.getMidTerm() != null
            ? trend.getMidTerm().getViewpoint() : "数据不足";

        return String.format(
            "你是一位资深财经分析师。请基于以下三个维度的分析结果，对「%s」板块进行综合研判。\n\n" +
            "## 消息面分析\n" +
            "- 信号: %s\n" +
            "- 核心驱动: %s\n" +
            "- 潜在风险: %s\n\n" +
            "## 资金面分析\n" +
            "- 资金信号: %s\n" +
            "- 主力净流入: %.2f亿\n" +
            "- 连续流入天数: %d\n" +
            "- 热度评分: %d/100\n\n" +
            "## 趋势分析\n" +
            "- 趋势信号: %s\n" +
            "- 短期观点: %s\n" +
            "- 中期观点: %s\n\n" +
            "## 输出要求\n" +
            "请以JSON格式输出，包含以下字段：\n" +
            "1. overallRating: 综合评级，必须是以下之一：STRONG_BULLISH/BULLISH/NEUTRAL/BEARISH/STRONG_BEARISH\n" +
            "2. overallScore: 综合评分，范围0-100的整数\n" +
            "3. crossDimensionInsights: 跨维度关联洞察，数组格式，包含2-3条洞察\n" +
            "4. summary: 一句话总结，不超过50字\n\n" +
            "注意：\n" +
            "- 分析要客观，不要给出具体投资建议\n" +
            "- 重点关注维度间的关联和共振效应\n" +
            "- 如果信号矛盾，需要说明矛盾点",
            sectorName,
            policySignal,
            coreDrivers,
            potentialRisks,
            capitalSignal,
            mainNetInflow,
            consecutiveInflowDays,
            heatScore,
            trendSignal,
            shortTermView,
            midTermView
        );
    }

    /**
     * 解析LLM响应JSON
     */
    private SynthesisResult parseResponse(String json, SynthesisInput input) {
        try {
            JSONObject root = JSON.parseObject(json);

            // 解析评级
            String ratingStr = root.getString("overallRating");
            OverallRating rating;
            try {
                rating = OverallRating.valueOf(ratingStr != null ? ratingStr : "NEUTRAL");
            } catch (IllegalArgumentException e) {
                rating = OverallRating.NEUTRAL;
            }

            // 解析评分
            int score = root.getIntValue("overallScore", 50);
            score = Math.max(0, Math.min(100, score));

            // 解析洞察
            List<String> insights = new ArrayList<>();
            JSONArray insightsArray = root.getJSONArray("crossDimensionInsights");
            if (insightsArray != null) {
                for (int i = 0; i < insightsArray.size(); i++) {
                    insights.add(insightsArray.getString(i));
                }
            }

            // 解析总结
            String summary = root.getString("summary");
            if (summary == null || summary.isBlank()) {
                summary = "综合研判中";
            }

            // 构建四维信号灯
            DimensionSignals signals = buildDimensionSignals(input);

            return SynthesisResult.builder()
                .overallRating(rating)
                .overallScore(score)
                .dimensionSignals(signals)
                .crossDimensionInsights(insights)
                .summary(summary)
                .build();

        } catch (Exception e) {
            throw new RuntimeException("解析LLM响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建四维信号灯
     */
    private DimensionSignals buildDimensionSignals(SynthesisInput input) {
        PolicyImpact policy = input.getPolicyImpact();
        SectorHeat sector = input.getSectorHeat();
        TrendSignal trend = input.getTrendSignal();

        return DimensionSignals.builder()
            .news(policy != null ? policy.getSignal() : SignalType.NEUTRAL)
            .capital(sector != null ? sector.getCapitalSignal() : SignalType.NEUTRAL)
            .technical(trend != null ? trend.getSignal() : SignalType.NEUTRAL)
            .sentiment(sector != null ? sector.getSentimentSignal() : SignalType.NEUTRAL)
            .build();
    }
}

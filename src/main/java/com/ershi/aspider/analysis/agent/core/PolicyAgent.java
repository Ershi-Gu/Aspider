package com.ershi.aspider.analysis.agent.core;

import com.alibaba.fastjson2.JSON;
import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.agent.llm.LlmAnalysisExecutor;
import com.ershi.aspider.analysis.agent.llm.LlmExecutionException;
import com.ershi.aspider.analysis.agent.llm.LlmParseException;
import com.ershi.aspider.analysis.agent.llm.dto.PolicyLlmResponse;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptRenderException;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptTemplateNotFoundException;
import com.ershi.aspider.analysis.agent.rule.PolicyRuleEngine;
import com.ershi.aspider.analysis.retriever.domain.NewsRetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievedArticle;
import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息面分析Agent（LLM驱动 + 规则降级）
 *
 * 从新闻检索结果中提取政策信号、核心驱动因素和潜在风险
 * LLM调用失败时自动降级为规则分析
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class PolicyAgent implements Agent<AgentContext, PolicyImpact> {

    /** 最小证据数（用于置信度计算基准） */
    private static final int MIN_EVIDENCE_COUNT = 2;

    /** 相关新闻上限 */
    private static final int POLICY_NEWS_LIMIT = 5;

    private final LlmAnalysisExecutor llmExecutor;
    private final PolicyRuleEngine ruleEngine;

    public PolicyAgent(LlmAnalysisExecutor llmExecutor, PolicyRuleEngine ruleEngine) {
        this.llmExecutor = llmExecutor;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.POLICY;
    }

    @Override
    public PolicyImpact analyze(AgentContext context) {
        log.info("PolicyAgent 开始分析消息面");

        NewsRetrievalResult newsResult = context.getNewsResult();
        if (newsResult == null || newsResult.getArticles() == null || newsResult.getArticles().isEmpty()) {
            log.warn("无新闻数据，返回中性信号");
            return PolicyImpact.empty("no_news_data");
        }

        // 尝试LLM分析
        try {
            PolicyImpact result = analyzeLlm(context);
            log.info("PolicyAgent LLM分析成功，信号={}", result.getSignal());
            return result;
        } catch (PromptTemplateNotFoundException e) {
            log.warn("PolicyAgent Prompt模板缺失，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_PROMPT_MISSING);
        } catch (PromptRenderException e) {
            log.warn("PolicyAgent Prompt渲染失败，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_LLM_FALLBACK);
        } catch (LlmExecutionException e) {
            log.warn("PolicyAgent LLM执行失败，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_NON_AI);
        } catch (LlmParseException e) {
            log.warn("PolicyAgent LLM响应解析失败，降级为规则分析，原始响应: {}", e.getRawResponse(), e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_LLM_PARSE_ERROR);
        } catch (Exception e) {
            log.error("PolicyAgent 未知异常，降级为规则分析", e);
            return analyzeWithDegradation(context, AnalysisStatus.MSG_NON_AI);
        }
    }

    /**
     * LLM驱动分析
     */
    private PolicyImpact analyzeLlm(AgentContext context) {
        // 1. 构建Prompt变量
        Map<String, Object> variables = buildPromptVariables(context);

        // 2. 调用LLM
        PolicyLlmResponse llmResponse = llmExecutor.execute(
            AgentType.POLICY, variables, PolicyLlmResponse.class);

        // 3. 合并系统填充字段
        return mergeWithSystemData(llmResponse, context);
    }

    /**
     * 构建Prompt变量
     */
    private Map<String, Object> buildPromptVariables(AgentContext context) {
        String sectorName = context.getQuery() != null ? context.getQuery().getSectorName() : "未知板块";
        String analysisTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // 构建新闻列表JSON
        List<Map<String, Object>> newsList = context.getNewsResult().getArticles().stream()
            .limit(10)
            .map(article -> {
                Map<String, Object> item = new HashMap<>();
                item.put("title", article.getArticle().getTitle());
                item.put("summary", article.getArticle().getSummary());
                item.put("importance", article.getArticle().getImportance());
                item.put("newsType", article.getArticle().getNewsType() != null
                    ? article.getArticle().getNewsType().name() : "GENERAL");
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> variables = new HashMap<>();
        variables.put("sector_name", sectorName);
        variables.put("analysis_time", analysisTime);
        variables.put("news_list_json", JSON.toJSONString(newsList));
        return variables;
    }

    /**
     * 合并LLM输出与系统填充字段
     */
    private PolicyImpact mergeWithSystemData(PolicyLlmResponse llmResponse, AgentContext context) {
        List<RetrievedArticle> articles = context.getNewsResult().getArticles();

        // 提取政策类和高重要性新闻（系统填充relatedNews）
        List<RetrievedArticle> policyNews = articles.stream()
            .filter(a -> a.getArticle().getNewsType() == NewsTypeEnum.POLICY
                      || a.getArticle().getImportance() >= 3)
            .sorted(Comparator.comparingInt((RetrievedArticle a) -> a.getArticle().getImportance()).reversed())
            .limit(POLICY_NEWS_LIMIT)
            .toList();

        List<PolicyNewsItem> relatedNews = policyNews.stream()
            .map(this::toNewsItem)
            .toList();

        // 系统计算置信度
        double confidence = Math.min(1.0, policyNews.size() / (double) MIN_EVIDENCE_COUNT);

        return PolicyImpact.builder()
            .signal(llmResponse.getSignal() != null ? llmResponse.getSignal() : SignalType.NEUTRAL)
            .coreDrivers(llmResponse.getCoreDrivers() != null ? llmResponse.getCoreDrivers() : Collections.emptyList())
            .potentialRisks(llmResponse.getPotentialRisks() != null ? llmResponse.getPotentialRisks() : Collections.emptyList())
            .relatedNews(relatedNews)
            .confidence(confidence)
            .status(AnalysisStatus.normal())
            .build();
    }

    /**
     * 降级为规则分析
     */
    private PolicyImpact analyzeWithDegradation(AgentContext context, String degradeMessage) {
        PolicyImpact result = ruleEngine.analyze(context);
        result.setStatus(AnalysisStatus.degraded(degradeMessage));
        log.info("PolicyAgent 规则分析完成（降级），信号={}", result.getSignal());
        return result;
    }

    /**
     * 将检索文章转换为PolicyNewsItem
     */
    private PolicyNewsItem toNewsItem(RetrievedArticle article) {
        return PolicyNewsItem.builder()
            .title(article.getArticle().getTitle())
            .contentUrl(article.getArticle().getContentUrl())
            .publishTime(article.getArticle().getPublishTime())
            .importance(article.getArticle().getImportance() != null ? article.getArticle().getImportance() : 1)
            .newsType(article.getArticle().getNewsType())
            .build();
    }
}

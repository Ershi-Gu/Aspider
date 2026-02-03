package com.ershi.aspider.analysis.agent.core;

import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.retriever.domain.NewsRetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievedArticle;
import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息面分析Agent（规则驱动，不调用LLM）
 *
 * 从新闻检索结果中提取政策信号、核心驱动因素和潜在风险
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

    /** 正面关键词 */
    private static final List<String> POSITIVE_KEYWORDS = List.of(
        "利好", "上涨", "增长", "突破", "扶持", "补贴", "减税",
        "加速", "创新高", "超预期", "放量", "大涨", "政策支持",
        "国产替代", "自主可控", "产业升级"
    );

    /** 负面关键词 */
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
        "利空", "下跌", "下滑", "制裁", "限制", "收紧", "暴跌",
        "风险", "警告", "亏损", "减持", "退市", "监管", "处罚"
    );

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

        List<RetrievedArticle> articles = newsResult.getArticles();

        // 1. 提取政策类和高重要性新闻
        List<RetrievedArticle> policyNews = articles.stream()
            .filter(a -> a.getArticle().getNewsType() == NewsTypeEnum.POLICY
                      || a.getArticle().getImportance() >= 3)
            .sorted(Comparator.comparingInt((RetrievedArticle a) -> a.getArticle().getImportance()).reversed())
            .limit(POLICY_NEWS_LIMIT)
            .toList();

        // 2. 提取核心驱动因素（从高重要性新闻标题提取）
        List<String> coreDrivers = policyNews.stream()
            .filter(a -> a.getArticle().getImportance() >= 4)
            .map(a -> a.getArticle().getTitle())
            .limit(3)
            .toList();

        // 3. 识别潜在风险
        List<String> potentialRisks = extractRisks(articles);

        // 4. 判定信号：基于正面/负面关键词计数
        SignalType signal = determineSignal(policyNews);

        // 5. 计算置信度（基于证据数量）
        double confidence = Math.min(1.0, policyNews.size() / (double) MIN_EVIDENCE_COUNT);

        // 6. 构建相关新闻列表
        List<PolicyNewsItem> relatedNews = policyNews.stream()
            .map(this::toNewsItem)
            .toList();

        log.info("PolicyAgent 分析完成，信号={}，置信度={}", signal, confidence);

        return PolicyImpact.builder()
            .signal(signal)
            .coreDrivers(coreDrivers)
            .potentialRisks(potentialRisks)
            .relatedNews(relatedNews)
            .confidence(confidence)
            .status(AnalysisStatus.normal())
            .build();
    }

    /**
     * 判定消息面信号
     */
    private SignalType determineSignal(List<RetrievedArticle> news) {
        int positiveScore = 0;
        int negativeScore = 0;
        for (RetrievedArticle article : news) {
            String title = article.getArticle().getTitle();
            if (title == null) continue;
            if (containsAnyKeyword(title, POSITIVE_KEYWORDS)) positiveScore++;
            if (containsAnyKeyword(title, NEGATIVE_KEYWORDS)) negativeScore++;
        }

        if (positiveScore > negativeScore + 1) return SignalType.POSITIVE;
        if (negativeScore > positiveScore + 1) return SignalType.NEGATIVE;
        return SignalType.NEUTRAL;
    }

    /**
     * 从新闻中提取潜在风险因素
     */
    private List<String> extractRisks(List<RetrievedArticle> articles) {
        return articles.stream()
            .filter(a -> {
                String title = a.getArticle().getTitle();
                return title != null && containsAnyKeyword(title, NEGATIVE_KEYWORDS);
            })
            .map(a -> a.getArticle().getTitle())
            .limit(3)
            .collect(Collectors.toList());
    }

    /**
     * 检查文本是否包含指定关键词列表中的任一关键词
     */
    private boolean containsAnyKeyword(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

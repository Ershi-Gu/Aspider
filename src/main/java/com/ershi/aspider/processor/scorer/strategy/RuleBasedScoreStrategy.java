package com.ershi.aspider.processor.scorer.strategy;

import com.ershi.aspider.datasource.domain.FinancialArticle;
import com.ershi.aspider.datasource.domain.NewsTypeEnum;
import com.ershi.aspider.processor.scorer.config.ArticleScorerConfig;
import com.ershi.aspider.processor.scorer.domain.ArticleScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于规则的文章评分策略
 * <p>
 * 通过关键词匹配来判断文章的重要性和类型
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
@Component
public class RuleBasedScoreStrategy implements ArticleScoreStrategy {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedScoreStrategy.class);

    private final ArticleScorerConfig.RuleConfig ruleConfig;

    public RuleBasedScoreStrategy(ArticleScorerConfig config) {
        this.ruleConfig = config.getRule();
    }

    @Override
    public ScoreStrategyType getStrategyType() {
        return ScoreStrategyType.RULE;
    }

    @Override
    public ArticleScoreResult score(FinancialArticle article) {
        String title = article.getTitle();
        String content = article.getContent() != null ? article.getContent() : "";

        // 评估重要性
        int importance = evaluateImportance(title, content);

        // 评估新闻类型
        NewsTypeEnum newsType = evaluateNewsType(title, content);

        log.debug("文章评分完成：{} -> importance={}, newsType={}",
            title, importance, newsType);

        return new ArticleScoreResult(importance, newsType);
    }

    /**
     * 评估重要性等级
     */
    private int evaluateImportance(String title, String content) {
        String text = title + " " + content;

        // 优先匹配高级别
        if (containsAny(text, ruleConfig.getCriticalKeywords())) {
            return 5;
        }

        if (containsAny(text, ruleConfig.getImportantKeywords())) {
            return 4;
        }

        if (containsAny(text, ruleConfig.getAttentionKeywords())) {
            return 3;
        }

        // 标题中有政策/事件类关键词，至少为一般（2）
        if (containsAny(title, ruleConfig.getPolicyKeywords()) ||
            containsAny(title, ruleConfig.getEventKeywords())) {
            return 2;
        }

        // 默认普通
        return 1;
    }

    /**
     * 评估新闻类型
     */
    private NewsTypeEnum evaluateNewsType(String title, String content) {
        // 优先根据标题判断
        if (containsAny(title, ruleConfig.getPolicyKeywords())) {
            return NewsTypeEnum.POLICY;
        }

        if (containsAny(title, ruleConfig.getEventKeywords())) {
            return NewsTypeEnum.EVENT;
        }

        if (containsAny(title, ruleConfig.getIndustryKeywords())) {
            return NewsTypeEnum.INDUSTRY;
        }

        // 再根据内容判断
        String text = title + " " + content;

        if (containsAny(text, ruleConfig.getPolicyKeywords())) {
            return NewsTypeEnum.POLICY;
        }

        if (containsAny(text, ruleConfig.getEventKeywords())) {
            return NewsTypeEnum.EVENT;
        }

        if (containsAny(text, ruleConfig.getIndustryKeywords())) {
            return NewsTypeEnum.INDUSTRY;
        }

        return NewsTypeEnum.GENERAL;
    }

    /**
     * 检查文本是否包含关键词列表中的任意一个
     */
    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        return keywords.stream().anyMatch(text::contains);
    }
}
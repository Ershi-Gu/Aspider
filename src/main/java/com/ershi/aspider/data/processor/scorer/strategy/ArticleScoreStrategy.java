package com.ershi.aspider.data.processor.scorer.strategy;

import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.processor.scorer.domain.ArticleScoreResult;

/**
 * 文章评分策略接口
 * <p>
 * 支持多种评分策略：
 * <ul>
 *   <li>rule - 基于规则的关键词匹配评分</li>
 *   <li>llm - 基于LLM的语义理解评分（预留）</li>
 * </ul>
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
public interface ArticleScoreStrategy {

    /**
     * 获取策略类型
     *
     * @return 策略类型枚举
     */
    ScoreStrategyType getStrategyType();

    /**
     * 对文章进行评分
     *
     * @param article 待评分的文章
     * @return 评分结果（包含 importance 和 newsType）
     */
    ArticleScoreResult score(FinancialArticle article);
}
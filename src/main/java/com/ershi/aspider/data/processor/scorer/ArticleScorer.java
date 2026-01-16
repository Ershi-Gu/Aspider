package com.ershi.aspider.data.processor.scorer;

import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.processor.scorer.config.ArticleScorerConfig;
import com.ershi.aspider.data.processor.scorer.domain.ArticleScoreResult;
import com.ershi.aspider.data.processor.scorer.strategy.ArticleScoreStrategy;
import com.ershi.aspider.data.processor.scorer.strategy.ScoreStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章评分执行器
 * <p>
 * 根据配置选择评分策略，对文章进行重要性和类型评估
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
@Component
public class ArticleScorer {

    private static final Logger log = LoggerFactory.getLogger(ArticleScorer.class);

    private final ArticleScorerConfig config;
    private final Map<ScoreStrategyType, ArticleScoreStrategy> strategyMap;

    public ArticleScorer(ArticleScorerConfig config, List<ArticleScoreStrategy> strategies) {
        this.config = config;
        this.strategyMap = strategies.stream()
            .collect(Collectors.toMap(ArticleScoreStrategy::getStrategyType, Function.identity()));

        log.info("文章评分执行器初始化完成，可用策略: {}, 当前策略: {}",
            strategyMap.keySet(), config.getStrategy());
    }

    /**
     * 批量评分并应用到文章
     *
     * @param articles 待评分的文章列表
     */
    public void scoreBatch(List<FinancialArticle> articles) {
        log.info("开始批量评分，共 {} 条文章", articles.size());

        ArticleScoreStrategy strategy = getStrategy();

        for (FinancialArticle article : articles) {
            try {
                ArticleScoreResult result = strategy.score(article);
                article.setImportance(result.getImportance());
                article.setNewsType(result.getNewsType());
            } catch (Exception e) {
                log.warn("文章评分失败，使用默认值: {}", article.getTitle(), e);
                ArticleScoreResult defaultResult = ArticleScoreResult.defaultResult();
                article.setImportance(defaultResult.getImportance());
                article.setNewsType(defaultResult.getNewsType());
            }
        }

        // 统计评分结果
        logScoreStatistics(articles);
    }

    /**
     * 单个文章评分
     *
     * @param article 待评分的文章
     * @return 评分结果
     */
    public ArticleScoreResult score(FinancialArticle article) {
        return getStrategy().score(article);
    }

    /**
     * 获取当前配置的策略
     */
    private ArticleScoreStrategy getStrategy() {
        ScoreStrategyType strategyType = ScoreStrategyType.fromCode(config.getStrategy());
        ArticleScoreStrategy strategy = strategyType != null ? strategyMap.get(strategyType) : null;

        if (strategy == null) {
            log.warn("未找到策略 [{}]，使用默认规则策略", config.getStrategy());
            strategy = strategyMap.get(ScoreStrategyType.RULE);
        }

        return strategy;
    }

    /**
     * 记录评分统计信息，只作日志输出
     */
    private void logScoreStatistics(List<FinancialArticle> articles) {
        Map<Integer, Long> importanceStats = articles.stream()
            .collect(Collectors.groupingBy(FinancialArticle::getImportance, Collectors.counting()));

        Map<String, Long> typeStats = articles.stream()
            .collect(Collectors.groupingBy(
                a -> a.getNewsType().getCode(),
                Collectors.counting()
            ));

        log.info("评分统计 - 重要性分布: {}, 类型分布: {}", importanceStats, typeStats);
    }
}
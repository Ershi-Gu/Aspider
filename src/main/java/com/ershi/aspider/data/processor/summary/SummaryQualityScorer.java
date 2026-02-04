package com.ershi.aspider.data.processor.summary;

import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.SummaryQualityLevel;
import com.ershi.aspider.data.processor.summary.config.SummaryQualityConfig;
import com.ershi.aspider.data.processor.summary.domain.SummaryQualityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 摘要质量评分器
 * <p>
 * 对文章摘要进行多维度质量评估，输出评分和扣分原因。
 * 评分维度：空值检测、长度检查、标题重叠度、信息密度、模板噪声。
 *
 * @author Ershi-Gu
 */
@Component
public class SummaryQualityScorer {

    private static final Logger log = LoggerFactory.getLogger(SummaryQualityScorer.class);

    private final SummaryQualityConfig config;

    public SummaryQualityScorer(SummaryQualityConfig config) {
        this.config = config;
    }

    /**
     * 批量评分并应用到文章
     */
    public void scoreBatch(List<FinancialArticle> articles) {
        if (!config.getEnable()) {
            return;
        }
        log.info("开始摘要质量评分，共 {} 条", articles.size());

        int lowCount = 0;
        int mediumCount = 0;
        int highCount = 0;

        for (FinancialArticle article : articles) {
            SummaryQualityResult result = score(article);
            article.setSummaryQualityScore(result.getScore());
            article.setSummaryQualityLevel(result.getLevel());

            switch (result.getLevel()) {
                case LOW -> lowCount++;
                case MEDIUM -> mediumCount++;
                case HIGH -> highCount++;
            }
        }

        log.info("摘要质量评分完成：高质量={}, 中等={}, 低质量={}", highCount, mediumCount, lowCount);
    }

    /**
     * 单篇文章摘要质量评分
     */
    public SummaryQualityResult score(FinancialArticle article) {
        String summary = article.getSummary();

        if (summary == null || summary.trim().isEmpty()) {
            return SummaryQualityResult.emptySummary();
        }

        int totalScore = 100;
        List<String> reasons = new ArrayList<>();

        totalScore = checkLength(summary, totalScore, reasons);
        totalScore = checkTitleOverlap(summary, article.getTitle(), totalScore, reasons);
        totalScore = checkBoilerplate(summary, totalScore, reasons);
        totalScore = checkInformationDensity(summary, totalScore, reasons);

        totalScore = Math.max(0, Math.min(100, totalScore));

        SummaryQualityLevel level = SummaryQualityLevel.fromScore(
            totalScore, config.getHighQualityThreshold(), config.getLowQualityThreshold());

        boolean needLlm = level == SummaryQualityLevel.LOW;

        return SummaryQualityResult.builder()
            .score(totalScore)
            .level(level)
            .reasons(reasons)
            .needLlm(needLlm)
            .build();
    }

    /**
     * 长度检查
     */
    private int checkLength(String summary, int score, List<String> reasons) {
        int length = summary.length();

        if (length < config.getMinLength()) {
            reasons.add("摘要过短（" + length + "字）");
            return score - 40;
        }

        if (length > config.getMaxLength()) {
            reasons.add("摘要过长（" + length + "字）");
            return score - 15;
        }

        if (length < config.getIdealLengthMin()) {
            reasons.add("摘要偏短（" + length + "字）");
            return score - 10;
        }

        if (length > config.getIdealLengthMax()) {
            reasons.add("摘要偏长（" + length + "字）");
            return score - 5;
        }

        return score;
    }

    /**
     * 标题重叠度检查
     */
    private int checkTitleOverlap(String summary, String title, int score, List<String> reasons) {
        if (title == null || title.isEmpty()) {
            return score;
        }

        double similarity = calculateSimilarity(summary, title);
        if (similarity >= config.getTitleSimilarityThreshold()) {
            reasons.add("与标题高度重复（相似度" + String.format("%.0f%%", similarity * 100) + "）");
            return score - 30;
        }

        if (similarity >= 0.5) {
            reasons.add("与标题部分重复");
            return score - 10;
        }

        return score;
    }

    /**
     * 模板/噪声检查
     */
    private int checkBoilerplate(String summary, int score, List<String> reasons) {
        int hitCount = 0;
        for (String pattern : config.getBoilerplatePatterns()) {
            if (summary.contains(pattern)) {
                hitCount++;
            }
        }

        if (hitCount >= 3) {
            reasons.add("命中多个模板关键词");
            return score - 25;
        }

        if (hitCount > 0) {
            reasons.add("包含模板关键词");
            return score - hitCount * 8;
        }

        return score;
    }

    /**
     * 信息密度检查
     */
    private int checkInformationDensity(String summary, int score, List<String> reasons) {
        boolean hasNumbers = summary.matches(".*\\d+.*");
        boolean hasOrg = containsAny(summary, "公司", "集团", "银行", "委", "部", "局", "院");

        if (!hasNumbers && !hasOrg) {
            reasons.add("缺少具体数据或机构名称");
            return score - 10;
        }

        return score;
    }

    /**
     * 基于字符重叠的简易相似度计算
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0;
        }

        String shorter = text1.length() <= text2.length() ? text1 : text2;
        String longer = text1.length() > text2.length() ? text1 : text2;

        int matchCount = 0;
        for (int i = 0; i < shorter.length(); i++) {
            if (longer.indexOf(shorter.charAt(i)) >= 0) {
                matchCount++;
            }
        }

        return (double) matchCount / shorter.length();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

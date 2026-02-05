package com.ershi.aspider.data.processor.summary;

import com.ershi.aspider.common.utils.TextTruncateUtil;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import com.ershi.aspider.data.datasource.domain.SummaryQualityLevel;
import com.ershi.aspider.data.datasource.domain.SummarySourceEnum;
import com.ershi.aspider.data.processor.summary.service.LLMSummaryService;
import com.ershi.aspider.data.processor.summary.config.SummaryConfig;
import com.ershi.aspider.data.processor.summary.domain.SummaryQualityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 摘要处理器（统一入口）
 * <p>
 * 整合摘要提取、质量评估、LLM 生成的完整流程：
 * <ol>
 *   <li>备份原始摘要</li>
 *   <li>质量评分（若已有摘要）</li>
 *   <li>决策处理路径：高质量直接用、低质量/无摘要调LLM或提取</li>
 *   <li>输出最终 summary + 质量标记</li>
 * </ol>
 *
 * @author Ershi-Gu
 */
@Component
public class SummaryProcessor {

    private static final Logger log = LoggerFactory.getLogger(SummaryProcessor.class);

    private final SummaryConfig config;
    private final SummaryQualityScorer qualityScorer;
    private final SummaryExtractionStrategy extractionStrategy;
    private final LLMSummaryService llmService;

    public SummaryProcessor(SummaryConfig config,
                            SummaryQualityScorer qualityScorer,
                            SummaryExtractionStrategy extractionStrategy,
                            @Autowired(required = false) LLMSummaryService llmService) {
        this.config = config;
        this.qualityScorer = qualityScorer;
        this.extractionStrategy = extractionStrategy;
        this.llmService = llmService;
    }

    /**
     * 批量处理摘要（两阶段：先预扫描统计，再统一执行 LLM）
     */
    public void processBatch(List<FinancialArticle> articles) {
        log.info("开始摘要处理，共 {} 条", articles.size());

        // 预扫描，评估所有文章并制定处理计划
        List<ArticleProcessPlan> plans = new ArrayList<>(articles.size());
        for (FinancialArticle article : articles) {
            plans.add(createProcessPlan(article));
        }

        // 统计各类处理计划数量
        long highQualityCount = plans.stream().filter(p -> p.action == PlannedAction.KEEP).count();
        long needLlmCount = plans.stream().filter(p -> p.action == PlannedAction.LLM).count();
        long extractedCount = plans.stream().filter(p -> p.action == PlannedAction.EXTRACTED).count();
        long truncateCount = plans.stream().filter(p -> p.action == PlannedAction.TRUNCATE).count();

        int llmLimit = config.getLlm().getMaxPerBatch();
        long actualLlmCount = Math.min(needLlmCount, llmLimit);
        long exceedLimitCount = needLlmCount - actualLlmCount;

        log.info("摘要预扫描完成 | 高质量保留={}, 提取/截断={}, 需LLM生成={} (批次限额={}, 超额将截断={})",
                highQualityCount, extractedCount + truncateCount, needLlmCount, llmLimit, Math.max(0, exceedLimitCount));

        // 统一执行处理
        AtomicInteger llmSuccessCount = new AtomicInteger(0);
        int llmFailCount = 0;

        log.info("开始LLM摘要生成阶段 | 待处理={}, 批次限额={}", needLlmCount, llmLimit);
        for (ArticleProcessPlan plan : plans) {
            if (plan.action == PlannedAction.LLM) {
                // 统一执行 LLM 生成
                if (llmSuccessCount.get() < llmLimit && regenerateWithLlm(plan.article, llmSuccessCount)) {
                    // LLM 生成成功
                } else {
                    // LLM 失败或超限额，回退截断
                    llmFailCount++;
                    String fallback = TextTruncateUtil.smartTruncate(
                        plan.article.getContent(), config.getExtraction().getTruncateLength());
                    plan.article.setSummary(fallback);
                    plan.article.setSummarySource(SummarySourceEnum.TRUNCATED);
                    scoreIfEnabled(plan.article);
                }
            }
            // KEEP / EXTRACTED / TRUNCATE 均已在预扫描阶段完成，无需额外处理
        }

        log.info("摘要处理完成 | 高质量保留={}, 提取/截断={}, LLM生成成功={}, LLM失败回退={}",
                highQualityCount, extractedCount + truncateCount, llmSuccessCount.get(), llmFailCount);
    }

    /**
     * 预扫描：为单篇文章创建处理计划（不执行 LLM 调用）
     */
    private ArticleProcessPlan createProcessPlan(FinancialArticle article) {
        backupRawSummary(article);

        if (hasSummary(article)) {
            return planForExistingSummary(article);
        } else {
            return planForNoSummary(article);
        }
    }

    /**
     * 为已有摘要的文章制定处理计划
     */
    private ArticleProcessPlan planForExistingSummary(FinancialArticle article) {
        // 质量评估未启用，直接保留
        if (!config.getEnableQuality()) {
            article.setSummarySource(SummarySourceEnum.RAW);
            return ArticleProcessPlan.of(article, PlannedAction.KEEP, "质量评估未启用");
        }

        // 执行质量评分
        SummaryQualityResult quality = qualityScorer.score(article);
        article.setSummaryQualityScore(quality.getScore());
        article.setSummaryQualityLevel(quality.getLevel());

        // 高质量直接保留
        if (quality.getLevel() == SummaryQualityLevel.HIGH) {
            article.setSummarySource(SummarySourceEnum.RAW);
            return ArticleProcessPlan.of(article, PlannedAction.KEEP,
                    String.format("高质量(score=%d)", quality.getScore()));
        }

        // 判断是否需要 LLM
        boolean shouldUseLlm = quality.getLevel() == SummaryQualityLevel.LOW
            || (quality.getLevel() == SummaryQualityLevel.MEDIUM && isHighValue(article))
            || (config.getLlm().getForceHighValue() && isHighValue(article));

        if (shouldUseLlm) {
            String reason = String.format("%s(score=%d, reasons=%s)",
                    quality.getLevel(), quality.getScore(), quality.getReasons());
            return ArticleProcessPlan.of(article, PlannedAction.LLM, reason);
        }

        // 中等质量但不触发 LLM，保留原始摘要
        article.setSummarySource(SummarySourceEnum.RAW);
        return ArticleProcessPlan.of(article, PlannedAction.KEEP,
                String.format("中等质量保留(score=%d)", quality.getScore()));
    }

    /**
     * 为无摘要的文章制定处理计划
     */
    private ArticleProcessPlan planForNoSummary(FinancialArticle article) {
        SummaryExtractionStrategy.ExtractionResult extraction = extractionStrategy.extract(article);

        // 提取成功，直接使用提取结果
        if (extraction.hasSummary()) {
            article.setSummary(extraction.getSummary());
            article.setSummarySource(extraction.getSource());
            scoreIfEnabled(article);
            return ArticleProcessPlan.of(article, PlannedAction.EXTRACTED, "提取成功");
        }

        // 需要 LLM（长文本无法直接提取）
        if (extraction.isNeedLlm()) {
            return ArticleProcessPlan.of(article, PlannedAction.LLM, "长文本无摘要需LLM生成");
        }

        // 无法提取也不需要 LLM（如内容为空），截断兜底
        String fallback = TextTruncateUtil.smartTruncate(
            article.getContent(), config.getExtraction().getTruncateLength());
        article.setSummary(fallback);
        article.setSummarySource(SummarySourceEnum.TRUNCATED);
        scoreIfEnabled(article);
        return ArticleProcessPlan.of(article, PlannedAction.TRUNCATE, "截断兜底");
    }

    /**
     * 使用 LLM 重新生成摘要
     */
    private boolean regenerateWithLlm(FinancialArticle article, AtomicInteger llmCount) {
        if (llmService == null || !config.getLlm().getEnable()) {
            return false;
        }

        String content = article.getContent();
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        try {
            String newSummary = llmService.generateSummary(content);
            if (newSummary != null && !newSummary.trim().isEmpty()) {
                article.setSummary(newSummary);
                article.setSummarySource(SummarySourceEnum.LLM);
                llmCount.incrementAndGet();
                scoreIfEnabled(article);
                log.debug("LLM 摘要生成成功：{}", article.getTitle());
                return true;
            }
        } catch (Exception e) {
            log.warn("LLM 摘要生成失败：{}", article.getTitle(), e);
        }
        return false;
    }

    /**
     * 备份原始摘要
     */
    private void backupRawSummary(FinancialArticle article) {
        if (article.getSummary() != null && article.getSummaryRaw() == null) {
            article.setSummaryRaw(article.getSummary());
        }
    }

    /**
     * 若启用质量评估则评分
     */
    private void scoreIfEnabled(FinancialArticle article) {
        if (config.getEnableQuality()) {
            SummaryQualityResult quality = qualityScorer.score(article);
            article.setSummaryQualityScore(quality.getScore());
            article.setSummaryQualityLevel(quality.getLevel());
        }
    }

    private boolean hasSummary(FinancialArticle article) {
        return article.getSummary() != null && !article.getSummary().trim().isEmpty();
    }

    private boolean isHighValue(FinancialArticle article) {
        if (article.getImportance() != null && article.getImportance() >= 3) {
            return true;
        }
        return article.getNewsType() == NewsTypeEnum.POLICY || article.getNewsType() == NewsTypeEnum.EVENT;
    }

    /**
     * 计划执行的动作类型
     */
    private enum PlannedAction {
        /** 保留原始摘要（高质量或质量评估未启用） */
        KEEP,
        /** 已提取摘要 */
        EXTRACTED,
        /** 已截断作为摘要 */
        TRUNCATE,
        /** 需要 LLM 生成 */
        LLM
    }

    /**
     * 文章处理计划（预扫描阶段产出）
     */
    private static class ArticleProcessPlan {
        final FinancialArticle article;
        final PlannedAction action;
        final String reason;

        private ArticleProcessPlan(FinancialArticle article, PlannedAction action, String reason) {
            this.article = article;
            this.action = action;
            this.reason = reason;
        }

        static ArticleProcessPlan of(FinancialArticle article, PlannedAction action, String reason) {
            return new ArticleProcessPlan(article, action, reason);
        }
    }
}

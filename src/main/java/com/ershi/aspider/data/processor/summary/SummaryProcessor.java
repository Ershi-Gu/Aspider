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
     * 批量处理摘要
     */
    public void processBatch(List<FinancialArticle> articles) {
        log.info("开始摘要处理，共 {} 条", articles.size());

        AtomicInteger llmCount = new AtomicInteger(0);
        int llmLimit = config.getLlm().getMaxPerBatch();

        int highQuality = 0;
        int llmGenerated = 0;
        int extracted = 0;

        for (FinancialArticle article : articles) {
            ProcessResult result = process(article, llmCount, llmLimit);
            switch (result) {
                case HIGH_QUALITY -> highQuality++;
                case LLM_GENERATED -> llmGenerated++;
                case EXTRACTED -> extracted++;
            }
        }

        log.info("摘要处理完成：高质量保留={}, LLM生成={}, 提取/截断={}", highQuality, llmGenerated, extracted);
    }

    /**
     * 单篇文章摘要处理
     */
    private ProcessResult process(FinancialArticle article, AtomicInteger llmCount, int llmLimit) {
        backupRawSummary(article);

        if (hasSummary(article)) {
            return processExistingSummary(article, llmCount, llmLimit);
        } else {
            return processNoSummary(article, llmCount, llmLimit);
        }
    }

    /**
     * 处理已有摘要的文章
     */
    private ProcessResult processExistingSummary(FinancialArticle article, AtomicInteger llmCount, int llmLimit) {
        if (!config.getEnableQuality()) {
            article.setSummarySource(SummarySourceEnum.RAW);
            return ProcessResult.HIGH_QUALITY;
        }

        SummaryQualityResult quality = qualityScorer.score(article);
        article.setSummaryQualityScore(quality.getScore());
        article.setSummaryQualityLevel(quality.getLevel());

        if (quality.getLevel() == SummaryQualityLevel.HIGH) {
            article.setSummarySource(SummarySourceEnum.RAW);
            return ProcessResult.HIGH_QUALITY;
        }

        boolean shouldUseLlm = quality.getLevel() == SummaryQualityLevel.LOW
            || (quality.getLevel() == SummaryQualityLevel.MEDIUM && isHighValue(article))
            || (config.getLlm().getForceHighValue() && isHighValue(article));

        if (shouldUseLlm && llmCount.get() < llmLimit) {
            if (regenerateWithLlm(article, llmCount)) {
                return ProcessResult.LLM_GENERATED;
            }
        }

        article.setSummarySource(SummarySourceEnum.RAW);
        return ProcessResult.HIGH_QUALITY;
    }

    /**
     * 处理无摘要的文章
     */
    private ProcessResult processNoSummary(FinancialArticle article, AtomicInteger llmCount, int llmLimit) {
        SummaryExtractionStrategy.ExtractionResult extraction = extractionStrategy.extract(article);

        if (extraction.hasSummary()) {
            article.setSummary(extraction.getSummary());
            article.setSummarySource(extraction.getSource());
            scoreIfEnabled(article);
            return ProcessResult.EXTRACTED;
        }

        if (extraction.isNeedLlm() && llmCount.get() < llmLimit) {
            if (regenerateWithLlm(article, llmCount)) {
                return ProcessResult.LLM_GENERATED;
            }
        }

        String fallback = TextTruncateUtil.smartTruncate(
            article.getContent(), config.getExtraction().getTruncateLength());
        article.setSummary(fallback);
        article.setSummarySource(SummarySourceEnum.TRUNCATED);
        scoreIfEnabled(article);
        return ProcessResult.EXTRACTED;
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

    private enum ProcessResult {
        HIGH_QUALITY,
        LLM_GENERATED,
        EXTRACTED
    }
}

package com.ershi.aspider.data.processor.summary;

import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import com.ershi.aspider.data.datasource.domain.SummaryQualityLevel;
import com.ershi.aspider.data.datasource.domain.SummarySourceEnum;
import com.ershi.aspider.data.processor.extractor.service.LLMSummaryExecutor;
import com.ershi.aspider.data.processor.summary.config.SummaryQualityConfig;
import com.ershi.aspider.data.processor.summary.domain.SummaryQualityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 摘要质量决策服务
 * <p>
 * 根据质量评分和文章价值决定是否触发LLM重新生成摘要。
 * 支持批次LLM上限控制，超限回退到原始摘要。
 *
 * @author Ershi-Gu
 */
@Service
public class SummaryQualityDecisionService {

    private static final Logger log = LoggerFactory.getLogger(SummaryQualityDecisionService.class);

    private final SummaryQualityConfig config;
    private final SummaryQualityScorer scorer;
    private final LLMSummaryExecutor llmSummaryExecutor;

    public SummaryQualityDecisionService(SummaryQualityConfig config,
                                          SummaryQualityScorer scorer,
                                          @Autowired(required = false) LLMSummaryExecutor llmSummaryExecutor) {
        this.config = config;
        this.scorer = scorer;
        this.llmSummaryExecutor = llmSummaryExecutor;
    }

    /**
     * 批量处理摘要质量优化
     * <p>
     * 流程：备份原始摘要 → 质量评分 → 决策LLM触发 → 更新摘要
     */
    public void processBatch(List<FinancialArticle> articles) {
        if (!config.getEnable()) {
            log.debug("摘要质量评估未启用，跳过");
            return;
        }

        log.info("开始摘要质量优化处理，共 {} 条", articles.size());

        backupRawSummaries(articles);
        scorer.scoreBatch(articles);

        AtomicInteger llmCount = new AtomicInteger(0);
        int llmLimit = config.getMaxLlmPerBatch();

        for (FinancialArticle article : articles) {
            if (shouldTriggerLlm(article) && llmCount.get() < llmLimit) {
                regenerateSummary(article, llmCount);
            }
        }

        log.info("摘要质量优化完成，LLM生成 {} 条", llmCount.get());
    }

    /**
     * 备份原始摘要
     */
    private void backupRawSummaries(List<FinancialArticle> articles) {
        for (FinancialArticle article : articles) {
            if (article.getSummary() != null && article.getSummaryRaw() == null) {
                article.setSummaryRaw(article.getSummary());
            }
        }
    }

    /**
     * 判断是否需要触发LLM重新生成
     */
    private boolean shouldTriggerLlm(FinancialArticle article) {
        if (llmSummaryExecutor == null) {
            return false;
        }

        SummaryQualityLevel level = article.getSummaryQualityLevel();
        if (level == null) {
            return false;
        }

        if (level == SummaryQualityLevel.LOW) {
            return true;
        }

        if (level == SummaryQualityLevel.MEDIUM && isHighValue(article)) {
            return true;
        }

        if (config.getForceHighValue() && isHighValue(article)) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为高价值文章
     */
    private boolean isHighValue(FinancialArticle article) {
        if (article.getImportance() != null && article.getImportance() >= 3) {
            return true;
        }
        return article.getNewsType() == NewsTypeEnum.POLICY || article.getNewsType() == NewsTypeEnum.EVENT;
    }

    /**
     * 使用LLM重新生成摘要
     */
    private void regenerateSummary(FinancialArticle article, AtomicInteger llmCount) {
        String content = article.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.debug("文章内容为空，无法LLM生成摘要：{}", article.getTitle());
            return;
        }

        try {
            String newSummary = llmSummaryExecutor.generateSummary(content);
            if (newSummary != null && !newSummary.trim().isEmpty()) {
                article.setSummary(newSummary);
                article.setSummarySource(SummarySourceEnum.LLM);
                llmCount.incrementAndGet();

                SummaryQualityResult newResult = scorer.score(article);
                article.setSummaryQualityScore(newResult.getScore());
                article.setSummaryQualityLevel(newResult.getLevel());

                log.debug("LLM摘要生成成功：{}, 新评分={}", article.getTitle(), newResult.getScore());
            }
        } catch (Exception e) {
            log.warn("LLM摘要生成失败，保留原始摘要：{}", article.getTitle(), e);
        }
    }
}

package com.ershi.aspider.analysis.summary;

import com.ershi.aspider.analysis.retriever.domain.RetrievedArticle;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.SummarySourceEnum;
import com.ershi.aspider.data.processor.extractor.service.LLMSummaryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分析阶段摘要兜底服务
 * <p>
 * 对检索到的TopK文章进行摘要质量检查，低质量摘要实时生成。
 * 支持内存缓存，避免重复调用LLM。
 *
 * @author Ershi-Gu
 */
@Service
public class SummaryFallbackService {

    private static final Logger log = LoggerFactory.getLogger(SummaryFallbackService.class);

    private final SummaryFallbackConfig config;
    private final LLMSummaryExecutor llmSummaryExecutor;

    private final Map<String, String> summaryCache = new ConcurrentHashMap<>();

    public SummaryFallbackService(SummaryFallbackConfig config,
                                   @Autowired(required = false) LLMSummaryExecutor llmSummaryExecutor) {
        this.config = config;
        this.llmSummaryExecutor = llmSummaryExecutor;
    }

    /**
     * 对检索结果进行摘要兜底处理
     */
    public void processFallback(List<RetrievedArticle> articles) {
        if (!config.getEnable()) {
            return;
        }

        if (llmSummaryExecutor == null) {
            log.debug("LLM执行器未启用，跳过摘要兜底");
            return;
        }

        int limit = Math.min(articles.size(), config.getTopkLimit());
        AtomicInteger llmCount = new AtomicInteger(0);
        int maxLlm = config.getMaxLlmPerRequest();

        for (int i = 0; i < limit && llmCount.get() < maxLlm; i++) {
            RetrievedArticle retrieved = articles.get(i);
            FinancialArticle article = retrieved.getArticle();

            if (needFallback(article)) {
                generateFallbackSummary(article, llmCount);
            }
        }

        if (llmCount.get() > 0) {
            log.info("分析阶段摘要兜底完成，LLM生成 {} 条", llmCount.get());
        }
    }

    /**
     * 判断是否需要兜底
     */
    private boolean needFallback(FinancialArticle article) {
        if (article.getSummary() == null || article.getSummary().trim().isEmpty()) {
            return true;
        }

        Integer qualityScore = article.getSummaryQualityScore();
        return qualityScore != null && qualityScore < config.getMinQualityScore();
    }

    /**
     * 生成兜底摘要
     */
    private void generateFallbackSummary(FinancialArticle article, AtomicInteger llmCount) {
        String cacheKey = resolveCacheKey(article);

        String cachedSummary = summaryCache.get(cacheKey);
        if (cachedSummary != null) {
            article.setSummary(cachedSummary);
            article.setSummarySource(SummarySourceEnum.ANALYSIS_LLM);
            log.debug("使用缓存摘要：{}", article.getTitle());
            return;
        }

        String content = article.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.debug("文章内容为空，无法生成兜底摘要：{}", article.getTitle());
            return;
        }

        try {
            String newSummary = llmSummaryExecutor.generateSummary(content);
            if (newSummary != null && !newSummary.trim().isEmpty()) {
                article.setSummary(newSummary);
                article.setSummarySource(SummarySourceEnum.ANALYSIS_LLM);
                llmCount.incrementAndGet();

                putCache(cacheKey, newSummary);

                log.debug("分析兜底摘要生成成功：{}", article.getTitle());
            }
        } catch (Exception e) {
            log.warn("分析兜底摘要生成失败：{}", article.getTitle(), e);
        }
    }

    /**
     * 生成缓存Key
     */
    private String resolveCacheKey(FinancialArticle article) {
        if (article.getUniqueId() != null && !article.getUniqueId().isBlank()) {
            return article.getUniqueId();
        }
        String title = article.getTitle() != null ? article.getTitle() : "";
        String url = article.getContentUrl() != null ? article.getContentUrl() : "";
        return title + "|" + url;
    }

    /**
     * 缓存摘要（带容量限制）
     */
    private void putCache(String key, String summary) {
        if (summaryCache.size() >= config.getCache().getMaxSize()) {
            summaryCache.clear();
            log.debug("摘要缓存已满，清空重建");
        }
        summaryCache.put(key, summary);
    }
}

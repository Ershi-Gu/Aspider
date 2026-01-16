package com.ershi.aspider.data.processor.extractor;

import com.ershi.aspider.common.utils.TextTruncateUtil;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.processor.extractor.config.ContentExtractionConfig;
import com.ershi.aspider.data.processor.extractor.service.LLMSummaryExecutor;
import com.ershi.aspider.data.processor.extractor.strategy.HybridExtractionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容提取器
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
@Component
public class ContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(ContentExtractor.class);

    /** 摘要提取策略 */
    private final HybridExtractionStrategy strategy;

    /** 摘要提取配置 */
    private final ContentExtractionConfig config;

    /** LLM摘要执行器，由enable配置控制是否加载 */
    private final LLMSummaryExecutor llmSummaryExecutor;

    public ContentExtractor(HybridExtractionStrategy strategy, ContentExtractionConfig config,
        @Autowired(required = false) LLMSummaryExecutor llmSummaryExecutor) {
        this.strategy = strategy;
        this.config = config;
        this.llmSummaryExecutor = llmSummaryExecutor;
    }

    /**
     * 批量提取内容
     */
    public List<String> extractBatch(List<FinancialArticle> items) {
        log.info("开始批量提取内容，共 {} 条", items.size());

        return items.stream()
            .map(this::extract)
            .collect(Collectors.toList());
    }

    /**
     * 单个提取
     */
    public String extract(FinancialArticle item) {
        // 中短文本提取
        String extracted = strategy.extract(item);

        // 长文本提取
        if (extracted == null) {
            if (llmSummaryExecutor != null) {
                log.debug("长文本使用LLM总结：{}", item.getTitle());
                return llmSummaryExecutor.generateSummary(item.getContent());
            } else {
                log.debug("长文本截取（LLM未启用）：{}", item.getTitle());
                return TextTruncateUtil.smartTruncate(item.getContent(), config.getMediumTextTruncateLength());
            }
        }

        return extracted;
    }
}

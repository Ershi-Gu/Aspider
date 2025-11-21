package com.ershi.aspider.processor.extractor;

import com.ershi.aspider.common.utils.TextTruncateUtil;
import com.ershi.aspider.datasource.domain.NewsDataItem;
import com.ershi.aspider.processor.extractor.config.ContentExtractionConfig;
import com.ershi.aspider.processor.extractor.service.LLMSummaryService;
import com.ershi.aspider.processor.extractor.strategy.HybridExtractionStrategy;
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

    /** LLM 摘要提取服务，可配置是否开启 */
    private final LLMSummaryService llmSummaryService;

    public ContentExtractor(HybridExtractionStrategy strategy, ContentExtractionConfig config,
        @Autowired(required = false) LLMSummaryService llmSummaryService) {
        this.strategy = strategy;
        this.config = config;
        this.llmSummaryService = llmSummaryService;
        log.info("内容提取器初始化完成，LLM总结：{}", config.getEnableLlmSummary() ? "启用" : "禁用");
    }

    /**
     * 批量提取内容
     */
    public List<String> extractBatch(List<NewsDataItem> items) {
        log.info("开始批量提取内容，共 {} 条", items.size());

        return items.stream()
            .map(this::extract)
            .collect(Collectors.toList());
    }

    /**
     * 单个提取
     */
    public String extract(NewsDataItem item) {
        String extracted = strategy.extract(item);

        if (extracted == null) {
            if (config.getEnableLlmSummary() && llmSummaryService != null) {
                log.debug("长文本使用LLM总结：{}", item.getTitle());
                return llmSummaryService.generateSummary(item.getContent());
            } else {
                log.debug("长文本截取（LLM未启用）：{}", item.getTitle());
                return TextTruncateUtil.smartTruncate(item.getContent(), config.getMediumTextTruncateLength());
            }
        }

        return extracted;
    }
}

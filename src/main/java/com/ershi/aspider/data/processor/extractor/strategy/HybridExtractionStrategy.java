package com.ershi.aspider.data.processor.extractor.strategy;

import com.ershi.aspider.common.utils.TextTruncateUtil;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.processor.extractor.config.ContentExtractionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 混合内容提取策略 <br>
 * 策略：
 * 1. 有摘要：直接返回摘要
 * 2. 短文本（<500字）：返回全文
 * 3. 中文本（500-2000字）：截取600字
 * 4. 长文本（>2000字）：使用LLM总结（可选）
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
@Component
public class HybridExtractionStrategy {

    private static final Logger log = LoggerFactory.getLogger(HybridExtractionStrategy.class);

    private final ContentExtractionConfig config;

    public HybridExtractionStrategy(ContentExtractionConfig config) {
        this.config = config;
    }

    /**
     * 提取用于向量化的内容
     */
    public String extract(FinancialArticle item) {
        // 优先使用摘要
        if (hasSummary(item)) {
            log.debug("使用摘要：{}", item.getTitle());
            return item.getSummary();
        }

        String content = item.getContent();
        int length = content.length();

        // 短文本，直接返回
        if (length <= config.getShortTextThreshold()) {
            log.debug("短文本，直接返回：{} 字", length);
            return content;
        }

        // 中文本，智能截取
        if (length <= config.getMediumTextThreshold()) {
            log.debug("中文本，截取 {} 字，原长度 {}", config.getMediumTextTruncateLength(), length);
            return TextTruncateUtil.smartTruncate(content, config.getMediumTextTruncateLength());
        }

        log.debug("长文本，返回null等待LLM处理：{} 字", length);
        return null;
    }

    /**
     * 判断是否有有效摘要
     */
    private boolean hasSummary(FinancialArticle item) {
        return item.getSummary() != null && !item.getSummary().trim().isEmpty();
    }
}

package com.ershi.aspider.processor.extractor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 内容提取配置
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "processor.content-extraction")
public class ContentExtractionConfig {

    /** 短文本阈值（小于此长度直接返回全文） */
    private Integer shortTextThreshold = 500;

    /** 中文本阈值（小于此长度进行截取） */
    private Integer mediumTextThreshold = 2000;

    /** 中文本截取长度 */
    private Integer mediumTextTruncateLength = 600;

    /** 长文本阈值（大于此长度使用LLM总结） */
    private Integer longTextThreshold = 2000;

    /** 是否启用LLM总结（长文本） */
    private Boolean enableLlmSummary = false;

    /** LLM总结的目标长度 */
    private Integer llmSummaryTargetLength = 200;
}



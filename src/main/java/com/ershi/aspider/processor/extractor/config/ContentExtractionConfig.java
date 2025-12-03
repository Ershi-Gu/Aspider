package com.ershi.aspider.processor.extractor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 内容提取配置，用于分析数据摘要提取，字段后配置默认值
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

    /** LLM摘要配置 */
    private LlmSummary llmSummary = new LlmSummary();

    /**
     * LLM摘要配置内部类
     */
    @Data
    public static class LlmSummary {
        /** 是否启用 */
        private Boolean enable = false;

        /** 目标长度 */
        private Integer targetLength = 200;

        /** 提供商：qwen/zhipu/openai/deepseek */
        private String provider = "qwen";

        /** 模型名称 */
        private String model = "qwen3-max";

        /** API密钥 */
        private String apiKey;
    }
}



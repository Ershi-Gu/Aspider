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
    /** 摘要提取prompt */
    public static final String SUMMARY_PROMPT_TEMPLATE =
        """
         你是专业财经摘要生成器。
          ## 任务
          将文章压缩为%d字左右的摘要
          ## 规则
          - 首句点明核心事件/现象
          - 保留：具体数字、政策文号、公司/机构名称、因果关系
          - 删除：背景铺垫、重复表述、过渡句
          - 语气：客观陈述，不加评论词（如"值得关注"、"令人瞩目"）
          ## 禁止
          - 不要输出"摘要："等前缀
          - 不要分点罗列
          - 不要超出字数限制20%%以上
          ## 文章
          %s
         """;

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

        /** 提供商 */
        private String provider = "gemini";

        /** 模型名称 */
        private String model = "gemini-2.5-flash";

        /** API密钥 */
        private String apiKey;

        /** 自定义API转发地址（可选，不配置则使用官方地址） */
        private String baseUrl;
    }
}



package com.ershi.aspider.data.processor.summary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 摘要处理统一配置
 * <p>
 * 合并原 ContentExtractionConfig 和 SummaryQualityConfig，
 * 统一管理摘要提取、质量评估、LLM调用等配置。
 *
 * @author Ershi-Gu
 */
@Data
@Component
@ConfigurationProperties(prefix = "processor.summary")
public class SummaryConfig {

    /** 摘要提取 Prompt 模板 */
    public static final String SUMMARY_PROMPT_TEMPLATE =
        "你是专业财经摘要生成器。\n" +
        "## 任务\n" +
        "将文章压缩为%d字左右的摘要\n" +
        "## 规则\n" +
        "- 首句点明核心事件/现象\n" +
        "- 保留：具体数字、政策文号、公司/机构名称、因果关系\n" +
        "- 删除：背景铺垫、重复表述、过渡句\n" +
        "- 语气：客观陈述，不加评论词（如\"值得关注\"、\"令人瞩目\"）\n" +
        "- 输出为一段连续文字，不要换行或空行\n" +
        "## 禁止\n" +
        "- 不要输出\"摘要：\"等前缀\n" +
        "- 不要分点罗列\n" +
        "- 不要分段或插入空行\n" +
        "- 不要超出字数限制20%%以上\n" +
        "## 文章\n" +
        "%s";

    /** 是否启用摘要质量评估与优化 */
    private Boolean enableQuality = false;

    /** 提取配置 */
    private Extraction extraction = new Extraction();

    /** 质量评估配置 */
    private Quality quality = new Quality();

    /** LLM 配置 */
    private Llm llm = new Llm();

    /**
     * 提取配置
     */
    @Data
    public static class Extraction {
        /** 短文本阈值（小于此长度直接返回全文） */
        private Integer shortTextThreshold = 500;

        /** 中文本阈值（小于此长度进行截取） */
        private Integer mediumTextThreshold = 2000;

        /** 中文本截取长度 */
        private Integer truncateLength = 600;
    }

    /**
     * 质量评估配置
     */
    @Data
    public static class Quality {
        /** 低质量阈值（低于此分数强制触发LLM） */
        private Integer lowThreshold = 60;

        /** 高质量阈值（高于此分数无需处理） */
        private Integer highThreshold = 75;

        /** 摘要最小有效长度 */
        private Integer minLength = 30;

        /** 摘要理想最小长度 */
        private Integer idealLengthMin = 80;

        /** 摘要理想最大长度 */
        private Integer idealLengthMax = 200;

        /** 摘要最大长度 */
        private Integer maxLength = 400;

        /** 标题相似度阈值（超过此值判定为标题复用） */
        private Double titleSimilarityThreshold = 0.7;

        /** 模板/噪声关键词 */
        private List<String> boilerplatePatterns = List.of(
            "点击查看", "原标题", "来源：", "记者：", "编辑：",
            "转载请注明", "责任编辑", "本文转自"
        );
    }

    /**
     * LLM 配置（OpenAI 兼容格式）
     */
    @Data
    public static class Llm {
        /** 是否启用 LLM 摘要生成 */
        private Boolean enable = false;

        /** 摘要目标长度 */
        private Integer targetLength = 200;

        /** 模型名称 */
        private String model = "gpt-4o-mini";

        /** API 密钥 */
        private String apiKey;

        /** API 地址（OpenAI 兼容格式，如 https://api.openai.com/v1/chat/completions） */
        private String baseUrl = "https://api.openai.com/v1/chat/completions";

        /** 是否对高价值文章强制触发 LLM */
        private Boolean forceHighValue = false;

        /** 单批次 LLM 调用上限 */
        private Integer maxPerBatch = 40;
    }
}

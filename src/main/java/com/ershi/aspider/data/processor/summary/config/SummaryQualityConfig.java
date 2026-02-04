package com.ershi.aspider.data.processor.summary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 摘要质量评分配置
 *
 * @author Ershi-Gu
 */
@Data
@Component
@ConfigurationProperties(prefix = "processor.summary-quality")
public class SummaryQualityConfig {

    /** 是否启用摘要质量评估 */
    private Boolean enable = false;

    /** 低质量阈值（低于此分数强制触发LLM） */
    private Integer lowQualityThreshold = 60;

    /** 高质量阈值（高于此分数无需处理） */
    private Integer highQualityThreshold = 75;

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

    /** 是否对高价值文章强制触发LLM */
    private Boolean forceHighValue = false;

    /** 单批次LLM调用上限 */
    private Integer maxLlmPerBatch = 40;
}

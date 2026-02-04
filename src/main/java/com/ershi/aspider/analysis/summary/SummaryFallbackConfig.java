package com.ershi.aspider.analysis.summary;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分析阶段摘要兜底配置
 *
 * @author Ershi-Gu
 */
@Data
@Component
@ConfigurationProperties(prefix = "analysis.summary-fallback")
public class SummaryFallbackConfig {

    /** 是否启用分析阶段兜底 */
    private Boolean enable = false;

    /** 触发兜底的最低质量评分 */
    private Integer minQualityScore = 60;

    /** 兜底处理的TopK限制 */
    private Integer topkLimit = 10;

    /** 单次分析请求最大LLM调用数 */
    private Integer maxLlmPerRequest = 5;

    /** 缓存配置 */
    private CacheConfig cache = new CacheConfig();

    /** 是否写回ES */
    private Boolean writeBack = false;

    @Data
    public static class CacheConfig {
        /** 缓存类型：memory */
        private String type = "memory";

        /** 最大缓存条数 */
        private Integer maxSize = 2000;

        /** 缓存过期时间 */
        private String ttl = "12h";
    }
}

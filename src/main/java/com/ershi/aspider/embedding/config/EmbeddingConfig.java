package com.ershi.aspider.embedding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量化模块配置
 *
 * @author Ershi-Gu.
 * @since 2025-12-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {

    /** API限制：每次最多处理的文本条数 */
    private Integer maxBatchSize = 10;

    /** RPM限制：每分钟最大请求数 */
    private Integer rpmLimit = 60;
}

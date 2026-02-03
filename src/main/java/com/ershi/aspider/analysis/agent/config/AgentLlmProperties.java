package com.ershi.aspider.analysis.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent LLM 多API配置属性
 *
 * 支持全局默认配置 + 按Agent覆盖的配置模式
 * 每个字段独立合并：agent专用配置 > default配置
 *
 * @author Ershi-Gu
 */
@Data
@Component
@ConfigurationProperties(prefix = "analysis.agent.llm")
public class AgentLlmProperties {

    /** 全局默认配置 */
    private LlmConfig defaultConfig = new LlmConfig();

    /** PolicyAgent 专用配置（可选覆盖） */
    private LlmConfig policy;

    /** SectorAgent 专用配置（可选覆盖） */
    private LlmConfig sector;

    /** TrendAgent 专用配置（可选覆盖） */
    private LlmConfig trend;

    /** SynthesisAgent 专用配置（推荐覆盖，使用更强模型） */
    private LlmConfig synthesis;

    /**
     * LLM配置项
     */
    @Data
    public static class LlmConfig {

        /** API端点（OpenAI兼容格式） */
        private String baseUrl;

        /** API密钥 */
        private String apiKey;

        /** 模型名称 */
        private String model;

        /** 超时秒数 */
        private Integer timeout = 30;

        /** 最大重试次数 */
        private Integer maxRetries = 3;
    }
}

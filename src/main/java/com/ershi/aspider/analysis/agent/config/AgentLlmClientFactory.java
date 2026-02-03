package com.ershi.aspider.analysis.agent.config;

import com.ershi.aspider.analysis.agent.domain.AgentType;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent LLM客户端工厂
 *
 * 支持配置覆盖：agent专用配置 > default配置
 * 客户端按配置指纹缓存复用，避免重复初始化
 *
 * @author Ershi-Gu
 */
@Component
@Slf4j
public class AgentLlmClientFactory {

    private final AgentLlmProperties properties;

    /** 客户端缓存：按配置指纹复用 */
    private final Map<String, OpenAIClient> clientCache = new ConcurrentHashMap<>();

    public AgentLlmClientFactory(AgentLlmProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取指定Agent类型的LLM客户端
     * 支持配置覆盖：agent专用配置 > default配置
     *
     * @param agentType Agent类型
     * @return OpenAI客户端
     */
    public OpenAIClient getClient(AgentType agentType) {
        AgentLlmProperties.LlmConfig config = resolveConfig(agentType);
        validateConfig(config, agentType);

        String fingerprint = generateFingerprint(config);
        return clientCache.computeIfAbsent(fingerprint, key -> {
            log.info("初始化LLM客户端，Agent={}，端点={}，模型={}",
                     agentType, config.getBaseUrl(), config.getModel());
            return OpenAIOkHttpClient.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .build();
        });
    }

    /**
     * 获取指定Agent的配置（合并后）
     *
     * @param agentType Agent类型
     * @return 合并后的配置
     */
    public AgentLlmProperties.LlmConfig getConfig(AgentType agentType) {
        return resolveConfig(agentType);
    }

    /**
     * 配置合并：agent专用字段 > default字段
     */
    private AgentLlmProperties.LlmConfig resolveConfig(AgentType agentType) {
        AgentLlmProperties.LlmConfig defaults = properties.getDefaultConfig();
        AgentLlmProperties.LlmConfig override = switch (agentType) {
            case POLICY -> properties.getPolicy();
            case SECTOR -> properties.getSector();
            case TREND -> properties.getTrend();
            case SYNTHESIS -> properties.getSynthesis();
            default -> null;
        };

        if (override == null) {
            return defaults;
        }

        // 字段级合并
        AgentLlmProperties.LlmConfig merged = new AgentLlmProperties.LlmConfig();
        merged.setBaseUrl(firstNonBlank(override.getBaseUrl(), defaults.getBaseUrl()));
        merged.setApiKey(firstNonBlank(override.getApiKey(), defaults.getApiKey()));
        merged.setModel(firstNonBlank(override.getModel(), defaults.getModel()));
        merged.setTimeout(override.getTimeout() != null ? override.getTimeout() : defaults.getTimeout());
        merged.setMaxRetries(override.getMaxRetries() != null ? override.getMaxRetries() : defaults.getMaxRetries());
        return merged;
    }

    /**
     * 验证配置完整性
     */
    private void validateConfig(AgentLlmProperties.LlmConfig config, AgentType agentType) {
        if (isBlank(config.getBaseUrl()) || isBlank(config.getApiKey()) || isBlank(config.getModel())) {
            throw new IllegalStateException(
                "Agent [" + agentType + "] LLM配置不完整，请检查 analysis.agent.llm.default 或 " +
                agentType.name().toLowerCase() + " 配置");
        }
    }

    /**
     * 生成配置指纹（用于客户端缓存键）
     */
    private String generateFingerprint(AgentLlmProperties.LlmConfig config) {
        return config.getBaseUrl() + "|" + config.getApiKey().hashCode();
    }

    /**
     * 返回第一个非空字符串
     */
    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * 判断字符串是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

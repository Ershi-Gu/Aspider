package com.ershi.aspider.analysis.agent.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.ershi.aspider.analysis.agent.config.AgentLlmClientFactory;
import com.ershi.aspider.analysis.agent.config.AgentLlmProperties;
import com.ershi.aspider.analysis.agent.domain.AgentType;
import com.ershi.aspider.analysis.agent.llm.dto.PolicyLlmResponse;
import com.ershi.aspider.analysis.agent.llm.dto.SectorLlmResponse;
import com.ershi.aspider.analysis.agent.llm.dto.TrendLlmResponse;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptRenderer;
import com.ershi.aspider.analysis.agent.llm.prompt.PromptTemplateRepository;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ResponseFormatJsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM分析执行器
 *
 * 统一封装：Prompt加载 -> 渲染 -> LLM调用 -> JSON解析 -> 响应验证
 * 调用失败时抛出异常，由上层Agent捕获并触发降级
 * 支持响应验证和输入清理，防止注入攻击
 *
 * @author Ershi-Gu
 */
@Component
@Slf4j
public class LlmAnalysisExecutor {

    private final PromptTemplateRepository templateRepository;
    private final PromptRenderer promptRenderer;
    private final AgentLlmClientFactory clientFactory;
    private final LlmResponseValidator responseValidator;

    public LlmAnalysisExecutor(PromptTemplateRepository templateRepository,
                               PromptRenderer promptRenderer,
                               AgentLlmClientFactory clientFactory,
                               LlmResponseValidator responseValidator) {
        this.templateRepository = templateRepository;
        this.promptRenderer = promptRenderer;
        this.clientFactory = clientFactory;
        this.responseValidator = responseValidator;
    }

    /**
     * 执行LLM分析
     *
     * @param agentType    Agent类型（用于加载模板和获取客户端）
     * @param variables    Prompt变量映射
     * @param responseType 响应类型Class
     * @param <T>          响应类型
     * @return 解析后的响应对象
     * @throws LlmExecutionException LLM调用或解析失败时抛出
     * @throws LlmParseException     JSON解析失败时抛出（用于区分错误类型）
     */
    public <T> T execute(AgentType agentType, Map<String, Object> variables, Class<T> responseType) {
        log.info("LLM执行开始，AgentType={}", agentType);

        // 1. 清理输入变量（防止Prompt注入）
        Map<String, Object> sanitizedVariables = sanitizeVariables(variables);

        // 2. 加载并渲染Prompt
        String template = templateRepository.getTemplate(agentType);
        String prompt = promptRenderer.render(template, sanitizedVariables);

        log.debug("Prompt渲染完成，长度={}", prompt.length());

        // 3. 获取LLM客户端和配置
        OpenAIClient client = clientFactory.getClient(agentType);
        AgentLlmProperties.LlmConfig config = clientFactory.getConfig(agentType);

        // 4. 构建请求参数（强制JSON输出）
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(config.getModel())
            .addUserMessage(prompt)
            .responseFormat(ResponseFormatJsonObject.builder().build())
            .build();

        // 5. 调用LLM
        String responseJson;
        try {
            ChatCompletion completion = client.chat().completions().create(params);
            responseJson = completion.choices().get(0).message().content().orElse("");

            if (responseJson.isBlank()) {
                throw new LlmExecutionException("LLM响应内容为空");
            }

            log.debug("LLM响应获取成功，长度={}", responseJson.length());
        } catch (LlmExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmExecutionException("LLM调用失败: " + e.getMessage(), e);
        }

        // 6. 解析JSON响应
        T response;
        try {
            response = JSON.parseObject(responseJson, responseType);
            if (response == null) {
                throw new LlmParseException("LLM响应解析结果为空", responseJson);
            }
        } catch (JSONException e) {
            log.error("LLM响应JSON解析失败，原始响应：{}", responseJson);
            throw new LlmParseException("LLM响应JSON解析失败: " + e.getMessage(), responseJson, e);
        }

        // 7. 验证并修复响应
        response = validateAndFixResponse(response, responseType);

        log.info("LLM执行成功，AgentType={}", agentType);
        return response;
    }

    /**
     * 清理输入变量（防止Prompt注入）
     *
     * @param variables 原始变量映射
     * @return 清理后的变量映射
     */
    private Map<String, Object> sanitizeVariables(Map<String, Object> variables) {
        java.util.HashMap<String, Object> sanitized = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strValue) {
                // 清理字符串值：移除潜在的注入模式
                String cleaned = strValue
                    .replaceAll("\\{\\{", "{")     // 防止模板变量注入
                    .replaceAll("\\}\\}", "}")
                    .replaceAll("(?i)ignore.*previous.*instruction", "") // 常见注入模式
                    .replaceAll("(?i)disregard.*above", "")              // 常见注入模式
                    .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", ""); // 控制字符
                sanitized.put(entry.getKey(), cleaned);
            } else {
                sanitized.put(entry.getKey(), value);
            }
        }
        return sanitized;
    }

    /**
     * 验证并修复响应
     *
     * @param response     原始响应
     * @param responseType 响应类型
     * @param <T>          响应类型
     * @return 验证并修复后的响应
     */
    @SuppressWarnings("unchecked")
    private <T> T validateAndFixResponse(T response, Class<T> responseType) {
        if (responseType == PolicyLlmResponse.class) {
            return (T) responseValidator.validateAndFix((PolicyLlmResponse) response);
        } else if (responseType == SectorLlmResponse.class) {
            return (T) responseValidator.validateAndFix((SectorLlmResponse) response);
        } else if (responseType == TrendLlmResponse.class) {
            return (T) responseValidator.validateAndFix((TrendLlmResponse) response);
        }
        // 其他类型不做验证，直接返回
        return response;
    }
}

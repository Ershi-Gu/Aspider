package com.ershi.aspider.data.processor.summary.service;

import com.ershi.aspider.data.processor.summary.config.SummaryConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM摘要服务（OpenAI兼容格式）
 * <p>
 * 统一使用 OpenAI API 格式，兼容：
 * OpenAI、Azure OpenAI、Qwen、DeepSeek、Moonshot 等遵循 OpenAI 格式的服务
 *
 * @author Ershi-Gu
 */
@Service
@ConditionalOnProperty(name = "processor.summary.llm.enable", havingValue = "true")
public class LLMSummaryService {

    private static final Logger log = LoggerFactory.getLogger(LLMSummaryService.class);

    private final SummaryConfig config;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;
    private Bucket bucket;

    public LLMSummaryService(SummaryConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        Integer rpmLimit = config.getLlm().getRpmLimit();
        this.bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(rpmLimit, Duration.ofMinutes(1)))
            .build();

        log.info("LLM摘要服务初始化完成，模型：{}，端点：{}，RPM限制：{}",
            config.getLlm().getModel(),
            config.getLlm().getBaseUrl(),
            rpmLimit);
    }

    /**
     * 生成摘要（使用配置的目标长度）
     */
    public String generateSummary(String content) {
        return generateSummary(content, config.getLlm().getTargetLength());
    }

    /**
     * 生成摘要（指定目标长度）
     */
    public String generateSummary(String content, int targetLength) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("输入内容为空，跳过摘要生成");
            return "";
        }

        try {
            String prompt = String.format(SummaryConfig.SUMMARY_PROMPT_TEMPLATE, targetLength, content);
            return callLlmApi(prompt);
        } catch (Exception e) {
            log.error("LLM摘要生成失败", e);
            return "";
        }
    }

    /**
     * 批量生成摘要
     */
    public List<String> batchGenerateSummary(List<String> contents) {
        return batchGenerateSummary(contents, config.getLlm().getTargetLength());
    }

    /**
     * 批量生成摘要（指定目标长度）
     */
    public List<String> batchGenerateSummary(List<String> contents, int targetLength) {
        if (contents == null || contents.isEmpty()) {
            return List.of();
        }

        log.info("开始批量生成摘要，共 {} 条", contents.size());

        return contents.stream()
            .map(content -> generateSummary(content, targetLength))
            .collect(Collectors.toList());
    }

    /**
     * 调用 OpenAI 兼容格式的 LLM API（带限流）
     */
    private String callLlmApi(String prompt) throws Exception {
        bucket.asBlocking().consume(1);

        SummaryConfig.Llm llmConfig = config.getLlm();

        Map<String, Object> requestBody = Map.of(
            "model", llmConfig.getModel(),
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(llmConfig.getBaseUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmConfig.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("LLM API 调用失败，状态码：{}，响应：{}", response.statusCode(), response.body());
            return "";
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");

        if (choices.isEmpty()) {
            log.error("LLM API 返回结果为空");
            return "";
        }

        String summary = choices.get(0).path("message").path("content").asText();
        log.debug("摘要生成成功，原文长度：{}，摘要长度：{}", prompt.length(), summary.length());
        return summary;
    }
}

package com.ershi.aspider.data.processor.summary.service;

import com.ershi.aspider.data.processor.summary.config.SummaryConfig;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM摘要服务（OpenAI SDK 实现）
 * <p>
 * 使用 OpenAI 官方 Java SDK，兼容：
 * OpenAI、Azure OpenAI、Qwen、DeepSeek、Moonshot 等遵循 OpenAI 格式的服务
 * <p>
 * 注意：base-url 配置应为 API 根路径（如 https://api.openai.com/v1），
 * SDK 会自动拼接 /chat/completions 等路径
 *
 * @author Ershi-Gu
 */
@Service
@ConditionalOnProperty(name = "processor.summary.llm.enable", havingValue = "true")
public class LLMSummaryService {

    private static final Logger log = LoggerFactory.getLogger(LLMSummaryService.class);

    private final SummaryConfig config;
    private OpenAIClient client;
    private Bucket bucket;

    public LLMSummaryService(SummaryConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        SummaryConfig.Llm llmConfig = config.getLlm();

        // 初始化 OpenAI SDK 客户端
        this.client = OpenAIOkHttpClient.builder()
            .baseUrl(llmConfig.getBaseUrl())
            .apiKey(llmConfig.getApiKey())
            .build();

        // 初始化 RPM 限流器
        Integer rpmLimit = llmConfig.getRpmLimit();
        this.bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(rpmLimit, Duration.ofMinutes(1)))
            .build();

        log.info("LLM摘要服务初始化完成，模型：{}，端点：{}，RPM限制：{}",
            llmConfig.getModel(),
            llmConfig.getBaseUrl(),
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
     * 调用 OpenAI SDK（带限流）
     */
    private String callLlmApi(String prompt) throws Exception {
        // 限流等待
        bucket.asBlocking().consume(1);

        SummaryConfig.Llm llmConfig = config.getLlm();

        // 构建请求参数
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(llmConfig.getModel())
            .addUserMessage(prompt)
            .temperature(0.3)
            .build();

        // 调用 LLM
        ChatCompletion completion = client.chat().completions().create(params);

        // 提取响应内容
        String summary = completion.choices().get(0).message().content().orElse("");

        if (summary.isBlank()) {
            log.error("LLM API 返回结果为空");
            return "";
        }

        log.debug("摘要生成成功，原文长度：{}，摘要长度：{}", prompt.length(), summary.length());
        return summary;
    }
}

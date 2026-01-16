package com.ershi.aspider.data.processor.extractor.service.impl;

import com.ershi.aspider.data.processor.extractor.config.ContentExtractionConfig;
import com.ershi.aspider.data.processor.extractor.domain.LLMProviderEnum;
import com.ershi.aspider.data.processor.extractor.service.LLMSummaryService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 谷歌Gemini LLM摘要服务实现
 *
 * @author Ershi-Gu.
 * @since 2025-12-04
 */
@Service
@ConditionalOnProperty(name = "processor.content-extraction.llm-summary.provider", havingValue = "gemini")
public class GeminiSummaryService implements LLMSummaryService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSummaryService.class);

    private final ContentExtractionConfig config;

    /** gemini sdk */
    private Client client;

    public GeminiSummaryService(ContentExtractionConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        Client.Builder clientBuilder = Client.builder()
            .apiKey(config.getLlmSummary().getApiKey());

        // 如果配置了自定义转发地址，则使用；否则使用官方默认地址
        String baseUrl = config.getLlmSummary().getBaseUrl();
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            clientBuilder.httpOptions(HttpOptions.builder()
                .baseUrl(baseUrl)
                .build());
            log.info("Gemini 使用自定义转发地址：{}", baseUrl);
        } else {
            log.info("Gemini 使用官方API地址");
        }

        client = clientBuilder.build();
        log.info("Gemini LLM摘要服务初始化成功，模型：{}", config.getLlmSummary().getModel());
    }

    @Override
    public LLMProviderEnum getProviderType() {
        return LLMProviderEnum.GEMINI;
    }

    @Override
    public String generateSummary(String content, int targetLength) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("输入内容为空，跳过摘要生成");
            return "";
        }

        try {
            String prompt = String.format(ContentExtractionConfig.SUMMARY_PROMPT_TEMPLATE,
                                          targetLength, content);

            // 调用 Gemini API 生成内容
            GenerateContentResponse response = client.models.generateContent(
                config.getLlmSummary().getModel(),
                prompt,
                null
            );

            // 获取响应文本
            String summary = response.text();
            if (summary == null || summary.isEmpty()) {
                log.error("Gemini LLM摘要生成失败，返回结果为空");
                return "";
            }

            log.debug("摘要生成成功，原文长度：{}，摘要长度：{}", content.length(), summary.length());
            return summary;

        } catch (Exception e) {
            log.error("调用Gemini LLM API生成摘要时发生异常：", e);
            return "";
        }
    }

    @Override
    public List<String> batchGenerateSummary(List<String> contents, int targetLength) {
        if (contents == null || contents.isEmpty()) {
            log.warn("输入内容列表为空，跳过批量摘要生成");
            return List.of();
        }

        log.info("开始批量生成摘要，共 {} 条", contents.size());

        return contents.stream()
            .map(content -> generateSummary(content, targetLength))
            .collect(Collectors.toList());
    }

}

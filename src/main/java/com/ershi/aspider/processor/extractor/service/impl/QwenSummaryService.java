package com.ershi.aspider.processor.extractor.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.ershi.aspider.processor.extractor.config.ContentExtractionConfig;
import com.ershi.aspider.processor.extractor.domain.LLMProviderEnum;
import com.ershi.aspider.processor.extractor.service.LLMSummaryService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云千问LLM摘要服务实现
 *
 * @author Ershi-Gu.
 * @since 2025-12-03
 */
@Service
@ConditionalOnProperty(name = "processor.content-extraction.llm-summary.provider", havingValue = "qwen")
public class QwenSummaryService implements LLMSummaryService {

    private static final Logger log = LoggerFactory.getLogger(QwenSummaryService.class);

    /** 摘要提取prompt */
    private static final String SUMMARY_PROMPT_TEMPLATE =
        "请将以下文章内容总结为%d字左右的摘要，要求：\n" +
            "1. 保留核心信息和关键数据\n" +
            "2. 使用简洁的语言\n" +
            "3. 只输出摘要内容，不要添加任何额外说明\n\n" +
            "文章内容：\n%s";

    private final ContentExtractionConfig config;

    /** 生成模型 */
    private Generation generation;

    public QwenSummaryService(ContentExtractionConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        generation = new Generation();
        log.info("千问LLM摘要服务初始化成功，模型：{}", config.getLlmSummary().getModel());
    }

    @Override
    public LLMProviderEnum getProviderType() {
        return LLMProviderEnum.QWEN;
    }

    @Override
    public String generateSummary(String content, int targetLength) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("输入内容为空，跳过摘要生成");
            return "";
        }

        try {
            String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, targetLength, content);

            // 问答内容编制
            Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build();

            // 设置问答参数
            GenerationParam param = GenerationParam.builder()
                .apiKey(config.getLlmSummary().getApiKey())
                .model(config.getLlmSummary().getModel())
                .messages(Arrays.asList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

            // 提交ai请求
            GenerationResult result = generation.call(param);

            if (result.getOutput() == null || result.getOutput().getChoices() == null ||
                result.getOutput().getChoices().isEmpty()) {
                log.error("QWEN LLM摘要生成失败，返回结果为空");
                return "";
            }

            String summary = result.getOutput().getChoices().getFirst().getMessage().getContent();
            log.debug("摘要生成成功，原文长度：{}，摘要长度：{}", content.length(), summary.length());
            return summary;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用千问LLM API生成摘要时发生异常：", e);
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
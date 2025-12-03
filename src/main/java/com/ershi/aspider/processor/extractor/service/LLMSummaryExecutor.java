package com.ershi.aspider.processor.extractor.service;

import com.ershi.aspider.processor.extractor.config.ContentExtractionConfig;
import com.ershi.aspider.processor.extractor.domain.LLMProviderEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM摘要执行器，统一管理LLM摘要服务的调用
 * 由 enable 配置控制是否加载
 *
 * @author Ershi-Gu.
 * @since 2025-12-03
 */
@Component
@ConditionalOnProperty(name = "processor.content-extraction.llm-summary.enable", havingValue = "true")
public class LLMSummaryExecutor {

    private static final Logger log = LoggerFactory.getLogger(LLMSummaryExecutor.class);

    private final LLMSummaryService llmSummaryService;

    private final ContentExtractionConfig config;

    public LLMSummaryExecutor(LLMSummaryService llmSummaryService, ContentExtractionConfig config) {
        this.llmSummaryService = llmSummaryService;
        this.config = config;
        log.info("LLM摘要执行器初始化完成，提供商：{}", getProviderType().getDescription());
    }

    /**
     * 获取当前提供商类型
     */
    public LLMProviderEnum getProviderType() {
        return llmSummaryService.getProviderType();
    }

    /**
     * 生成摘要（使用配置的目标长度）
     *
     * @param content 原始内容
     * @return 摘要文本
     */
    public String generateSummary(String content) {
        return llmSummaryService.generateSummary(content, config.getLlmSummary().getTargetLength());
    }

    /**
     * 生成摘要（指定目标长度）
     *
     * @param content      原始内容
     * @param targetLength 目标长度
     * @return 摘要文本
     */
    public String generateSummary(String content, int targetLength) {
        return llmSummaryService.generateSummary(content, targetLength);
    }

    /**
     * 批量生成摘要
     *
     * @param contents 原始内容列表
     * @return 摘要文本列表
     */
    public List<String> batchGenerateSummary(List<String> contents) {
        return llmSummaryService.batchGenerateSummary(contents, config.getLlmSummary().getTargetLength());
    }
}
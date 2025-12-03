package com.ershi.aspider.processor.extractor.service;

import com.ershi.aspider.processor.extractor.domain.LLMProviderEnum;

import java.util.List;

/**
 * LLM摘要生成服务接口
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
public interface LLMSummaryService {

    /**
     * 获取提供商类型
     *
     * @return {@link LLMProviderEnum }
     */
    LLMProviderEnum getProviderType();

    /**
     * 生成摘要
     *
     * @param content      原始内容
     * @param targetLength 目标长度
     * @return 摘要文本
     */
    String generateSummary(String content, int targetLength);

    /**
     * 批量生成摘要
     *
     * @param contents     原始内容列表
     * @param targetLength 目标长度
     * @return 摘要文本列表
     */
    List<String> batchGenerateSummary(List<String> contents, int targetLength);
}
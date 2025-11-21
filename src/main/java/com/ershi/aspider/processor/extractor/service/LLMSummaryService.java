package com.ershi.aspider.processor.extractor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * LLM摘要生成服务
 *
 * @author Ershi-Gu.
 * @since 2025-11-15
 */
@Service
@ConditionalOnProperty(name = "processor.content-extraction.enable-llm-summary", havingValue = "true")
public class LLMSummaryService {

    private static final Logger log = LoggerFactory.getLogger(LLMSummaryService.class);

    @Value("${processor.content-extraction.llm-model}")
    private static final String LLM_MODEL = "qwen";

    /**
     * todo LLM生成摘要
     */
    public String generateSummary(String content) {
        return null;
    }

}

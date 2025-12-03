package com.ershi.aspider.processor.extractor.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM提供商枚举
 *
 * @author Ershi-Gu.
 * @since 2025-12-03
 */
@Getter
public enum LLMProviderEnum {

    QWEN("qwen", "通义千问"),
    ZHIPU("zhipu", "智谱AI"),
    ;

    private final String type;
    private final String description;

    private static final Map<String, LLMProviderEnum> CACHE;

    static {
        CACHE = new HashMap<>();
        for (LLMProviderEnum provider : values()) {
            CACHE.put(provider.type, provider);
        }
    }

    LLMProviderEnum(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public static LLMProviderEnum getByType(String type) {
        return CACHE.get(type);
    }
}
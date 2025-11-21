package com.ershi.aspider.embedding.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量化提供商
 *
 * @author Ershi-Gu.
 * @since 2025-11-14
 */
@Getter
public enum EmbeddingProviderEnum {

    ZHIPU("zhipu", "智谱AI"),
    QWEN("qwen", "千问"),
    ;

    private final String type;
    private final String description;

    private static final Map<String, EmbeddingProviderEnum> CACHE;

    static {
        CACHE = new HashMap<>();
        for (EmbeddingProviderEnum provider : values()) {
            CACHE.put(provider.type, provider);
        }
    }

    EmbeddingProviderEnum(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public static EmbeddingProviderEnum getEnumByType(String type) {
        return CACHE.get(type);
    }
}

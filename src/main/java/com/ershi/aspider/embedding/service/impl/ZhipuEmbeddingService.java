package com.ershi.aspider.embedding.service.impl;

import com.ershi.aspider.embedding.domain.EmbeddingProviderEnum;
import com.ershi.aspider.embedding.service.EmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * todo 智普向量化实现
 *
 * @author Ershi-Gu.
 * @since 2025-11-14
 */
@ConditionalOnProperty(name = "embedding.provider", havingValue = "zhipu")
public class ZhipuEmbeddingService implements EmbeddingService {

    /** 向量维度 */
    public static final Integer DIMENSION = 1024;

    @Override
    public EmbeddingProviderEnum getProviderType() {
        return null;
    }

    @Override
    public List<Double> embed(String text) {
        return List.of();
    }

    @Override
    public List<List<Double>> batchEmbed(List<String> texts) {
        return List.of();
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
}

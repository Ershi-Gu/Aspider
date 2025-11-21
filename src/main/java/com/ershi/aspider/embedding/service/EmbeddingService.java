package com.ershi.aspider.embedding.service;

import com.ershi.aspider.embedding.domain.EmbeddingProviderEnum;

import java.util.List;

/**
 *
 * @author Ershi-Gu.
 * @since 2025-11-14
 */
public interface EmbeddingService {

    /**
     * 获取提供商类型
     */
    EmbeddingProviderEnum getProviderType();

    /**
     * 单文本向量化
     * @param text 待向量化文本
     * @return 向量数组
     */
    List<Double> embed(String text);

    /**
     * 批量文本向量化
     * @param texts 待向量化文本列表
     * @return 向量列表，每个元素对应一个文本的向量
     */
    List<List<Double>> batchEmbed(List<String> texts);

    /**
     * 获取向量维度
     *
     * @return int 向量维度
     */
    int getDimension();
}

package com.ershi.aspider.data.embedding.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.ershi.aspider.data.embedding.domain.EmbeddingProviderEnum;
import com.ershi.aspider.data.embedding.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 千问向量化实现
 *
 * @author Ershi-Gu.
 * @since 2025-11-14
 */
@Service
@ConditionalOnProperty(name = "embedding.provider", havingValue = "qwen")
public class QwenEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(QwenEmbeddingService.class);

    @Value("${embedding.qwen.api-key}")
    private String apiKey;

    @Value("${embedding.qwen.model:text-embedding-v4}")
    private String model;

    /** 向量维度 */
    public static final Integer DIMENSION = 1024;

    /** 向量化模型 */
    private TextEmbedding textEmbedding;

    @PostConstruct
    public void init() {
        textEmbedding = new TextEmbedding();
        log.info("千问向量化服务初始化成功，模型：{}", model);
    }

    @Override
    public EmbeddingProviderEnum getProviderType() {
        return EmbeddingProviderEnum.QWEN;
    }

    @Override
    public List<Double> embed(String text) {
        // 复用批量方法，提高代码复用性
        List<List<Double>> results = batchEmbed(Collections.singletonList(text));

        return results.getFirst();
    }

    @Override
    public List<List<Double>> batchEmbed(List<String> texts) {
        try {
            // 构造向量化请求参数
            TextEmbeddingParam param = TextEmbeddingParam
                .builder()
                .apiKey(apiKey)
                .model(model)
                .texts(texts)
                .dimension(DIMENSION)
                .build();

            // 向量化模型调用
            TextEmbeddingResult result = textEmbedding.call(param);

            // 提取并返回向量
            if (result == null || result.getOutput() == null
                || result.getOutput().getEmbeddings() == null) {
                log.error("Qwen 文本向量化失败，返回结果为空");
                throw new RuntimeException("Qwen 获取向量失败");
            }
            return result.getOutput().getEmbeddings().stream()
                .map(TextEmbeddingResultItem::getEmbedding)
                .collect(Collectors.toList());

        } catch (NoApiKeyException e) {
            log.error("调用Qwen Embedding API 时发生异常:", e);
            throw new RuntimeException("Qwen 获取向量失败", e);
        }
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
}

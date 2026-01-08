package com.ershi.aspider.embedding.service.impl;

import com.ershi.aspider.embedding.domain.EmbeddingProviderEnum;
import com.ershi.aspider.embedding.service.EmbeddingService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.CreateEmbeddingResponse;
import com.openai.models.EmbeddingCreateParams;
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
 * OpenAI兼容向量化实现
 * 支持 OpenAI 官方 API 及兼容 OpenAI 协议的第三方服务
 *
 * @author Ershi-Gu.
 * @since 2025-01-07
 */
@Service
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    @Value("${embedding.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${embedding.openai.api-key}")
    private String apiKey;

    @Value("${embedding.openai.model:text-embedding-3-small}")
    private String model;

    /** 向量维度 */
    public static final Integer DIMENSION = 1024;

    private OpenAIClient client;

    @PostConstruct
    public void init() {
        client = OpenAIOkHttpClient.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build();
        log.info("OpenAI向量化服务初始化成功，端点：{}，模型：{}，维度：{}", baseUrl, model, DIMENSION);
    }

    @Override
    public EmbeddingProviderEnum getProviderType() {
        return EmbeddingProviderEnum.OPENAI;
    }

    @Override
    public List<Double> embed(String text) {
        List<List<Double>> results = batchEmbed(Collections.singletonList(text));
        return results.getFirst();
    }

    @Override
    public List<List<Double>> batchEmbed(List<String> texts) {
        try {
            EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(model)
                .input(EmbeddingCreateParams.Input.ofArrayOfStrings(texts))
                .dimensions(DIMENSION)
                .build();

            CreateEmbeddingResponse response = client.embeddings().create(params);

            if (response.data() == null || response.data().isEmpty()) {
                log.error("OpenAI 文本向量化失败，返回结果为空");
                throw new RuntimeException("OpenAI 获取向量失败");
            }

            return response.data().stream()
                .map(embedding -> embedding.embedding())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("调用OpenAI Embedding API 时发生异常:", e);
            throw new RuntimeException("OpenAI 获取向量失败", e);
        }
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
}

package com.ershi.aspider.data.embedding.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.CreateEmbeddingResponse;
import com.openai.models.EmbeddingCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量化服务（OpenAI 兼容格式）
 * <p>
 * 统一使用 OpenAI Embedding API 格式，兼容：
 * OpenAI、Azure OpenAI、Qwen、智谱、Ollama 等遵循 OpenAI 格式的服务
 *
 * @author Ershi-Gu
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${embedding.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${embedding.api-key}")
    private String apiKey;

    @Value("${embedding.model:text-embedding-3-small}")
    private String model;

    @Value("${embedding.dimension:1024}")
    private Integer dimension;

    private OpenAIClient client;

    @PostConstruct
    public void init() {
        client = OpenAIOkHttpClient.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build();
        log.info("向量化服务初始化成功，端点：{}，模型：{}，维度：{}", baseUrl, model, dimension);
    }

    /**
     * 单文本向量化
     */
    public List<Double> embed(String text) {
        List<List<Double>> results = batchEmbed(Collections.singletonList(text));
        return results.getFirst();
    }

    /**
     * 批量文本向量化
     */
    public List<List<Double>> batchEmbed(List<String> texts) {
        try {
            EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(model)
                .input(EmbeddingCreateParams.Input.ofArrayOfStrings(texts))
                .dimensions(dimension)
                .build();

            CreateEmbeddingResponse response = client.embeddings().create(params);

            if (response.data() == null || response.data().isEmpty()) {
                log.error("文本向量化失败，返回结果为空");
                throw new RuntimeException("获取向量失败");
            }

            return response.data().stream()
                .map(embedding -> embedding.embedding())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("调用 Embedding API 时发生异常:", e);
            throw new RuntimeException("获取向量失败", e);
        }
    }

    /**
     * 获取向量维度
     */
    public int getDimension() {
        return dimension;
    }
}

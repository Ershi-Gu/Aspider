package com.ershi.aspider.data.embedding;

import com.ershi.aspider.common.utils.BatchUtils;
import com.ershi.aspider.data.embedding.config.EmbeddingConfig;
import com.ershi.aspider.data.embedding.service.EmbeddingService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 向量化执行器，提供统一的向量化执行接口
 *
 * @author Ershi-Gu.
 * @since 2025-11-14
 */
@Component
public class EmbeddingExecutor {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingExecutor.class);

    /** 向量化服务 */
    private final EmbeddingService embeddingService;

    /** 虚拟线程池 */
    private final Executor aspiderVirtualExecutor;

    /** 速率限制器 */
    private final Bucket bucket;

    /** 向量化配置 */
    private final EmbeddingConfig embeddingConfig;

    /**
     * Spring 会自动注入当前激活的 EmbeddingService 实现，通过 @ConditionalOnProperty 控制只有一个实现被加载
     */
    public EmbeddingExecutor(EmbeddingService embeddingService, Executor aspiderVirtualExecutor,
                             EmbeddingConfig embeddingConfig) {
        this.embeddingService = embeddingService;
        this.aspiderVirtualExecutor = aspiderVirtualExecutor;
        this.embeddingConfig = embeddingConfig;
        // RPM限制：每分钟最大请求数，使用令牌桶实现
        this.bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(embeddingConfig.getRpmLimit(), Duration.ofMinutes(1)))
            .build();
        log.info("向量化执行器初始化完成，当前使用：{}，RPM限制：{}",
                 embeddingService.getProviderType().getDescription(),
                 embeddingConfig.getRpmLimit());
    }

    /**
     * 获取当前激活的向量化服务
     */
    public EmbeddingService getActiveService() {
        return embeddingService;
    }

    /**
     * 向量化单个文本
     *
     * @param text
     * @return {@link List }<{@link Double }>
     */
    public List<Double> embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空，跳过向量化");
            return null;
        }
        return embeddingService.embed(text);
    }

    /**
     * 批量文本向量化
     *
     * @param texts
     * @return {@link List }<{@link List }<{@link Double }>>
     */
    public List<List<Double>> embedTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("输入文本列表为空，跳过向量化");
            return null;
        }

        int totalSize = texts.size();
        log.info("EmbeddingExecutor 开始向量化 {} 条文本，每批最多 {} 条，RPM 限制 {}", totalSize,
                 embeddingConfig.getMaxBatchSize(), embeddingConfig.getRpmLimit());

        // 数据分片
        List<List<String>> batches = BatchUtils.partition(texts, embeddingConfig.getMaxBatchSize());
        int batchCount = batches.size();
        log.info("数据已分为 {} 个批次", batchCount);

        // 并发执行文本向量化（使用虚拟线程）
        List<CompletableFuture<List<List<Double>>>> futures = new ArrayList<>();

        // 分批启动任务
        for (int i = 0; i < batchCount; i++) {
            final int batchIndex = i;
            final List<String> batch = batches.get(i);

            CompletableFuture<List<List<Double>>> future =
                CompletableFuture.supplyAsync(() -> {
                    // 限流式执行向量化，保证不被Embedding API限制
                    return processBatchWithRateLimit(batch, batchIndex,
                                                     batchCount);
                }, aspiderVirtualExecutor);

            futures.add(future);
        }

        // 等待所有批次完成并聚合结果
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            // 等待任务完成
            allOf.join();

            // 收集结果（按顺序）
            List<List<Double>> allVectors = new ArrayList<>();
            for (CompletableFuture<List<List<Double>>> future : futures) {
                allVectors.addAll(future.get());
            }

            log.info("向量化完成，共处理 {} 条文本", allVectors.size());
            return allVectors;

        } catch (Exception e) {
            throw new RuntimeException("批量向量化失败", e);
        }
    }

    /**
     * 处理单个批次（基于Bucket4j令牌桶的速率限制）
     *
     * @param batch 批次数据
     * @param batchIndex 批次索引
     * @param totalBatches 总批次数
     * @return 向量列表
     */
    private List<List<Double>> processBatchWithRateLimit(List<String> batch, int batchIndex, int totalBatches) {
        try {
            // 从令牌桶消费1个令牌（阻塞直到获得令牌，虚拟线程友好）
            bucket.asBlocking().consume(1);

            long startTime = System.currentTimeMillis();
            log.debug("批次 [{}/{}] 开始向量化，大小：{}",
                      batchIndex + 1, totalBatches, batch.size());

            // 调用向量化API
            List<List<Double>> vectors = embeddingService.batchEmbed(batch);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("批次 [{}/{}] 向量化完成，耗时：{} ms",
                      batchIndex + 1, totalBatches, duration);

            return vectors;

        } catch (Exception e) {
            // 只抛出，不打印，让最外层统一处理
            throw new RuntimeException("批次向量化失败", e);
        }
    }

    /**
     * 获取向量维度
     *
     * @return int
     */
    public int getDimension() {
        return embeddingService.getDimension();
    }
}

package com.ershi.aspider.data.job;

import com.ershi.aspider.data.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.data.orchestration.service.FinancialArticleDataService;
import com.ershi.aspider.data.storage.elasticsearch.service.FinancialArticleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时获取财经新闻数据job
 * <p>
 * 定时任务：
 * <ul>
 *   <li>每2小时采集新闻数据（采集即向量化）</li>
 *   <li>每日凌晨2:00分层清理90天前的低重要性数据</li>
 * </ul>
 *
 * @author Ershi-Gu.
 * @since 2025-11-12
 */
@Component
public class FinancialArticleDataJob {

    public static final Logger log = LoggerFactory.getLogger(FinancialArticleDataJob.class);

    /** 新闻数据保留天数（90天后进入冷数据阶段） */
    private static final int RETENTION_DAYS = 90;

    /** 冷数据保留的最低重要性阈值（importance >= 3 的重要新闻保留） */
    private static final int MIN_IMPORTANCE_FOR_RETENTION = 3;

    private final FinancialArticleDataService financialArticleDataService;
    private final FinancialArticleStorageService financialArticleStorageService;

    public FinancialArticleDataJob(FinancialArticleDataService financialArticleDataService,
                                   FinancialArticleStorageService financialArticleStorageService) {
        this.financialArticleDataService = financialArticleDataService;
        this.financialArticleStorageService = financialArticleStorageService;
    }

    /**
     * 定时任务：获取所有数据源数据并处理（采集即向量化）
     * 每2小时执行一次
     */
    @Async
    @Scheduled(cron = "0 0 0/2 * * ?")
    public void scheduledProcessAllDataSources() {
        log.info("定时任务启动：开始采集财经新闻数据");

        try {
            int savedCount = financialArticleDataService.processAllFinancialArticle();
            log.info("定时任务完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("定时任务执行失败：采集财经新闻数据", e);
        }
    }

    /**
     * 定时清理任务：每日凌晨2:00分层清理过期新闻数据
     * <p>
     * 清理规则：删除 crawlTime 早于90天且 importance < 3 的普通新闻
     */
    @Async
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanExpiredData() {
        log.info("定时清理任务启动：开始分层清理超过 {} 天的低重要性新闻数据", RETENTION_DAYS);

        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(RETENTION_DAYS);
            long deletedCount = financialArticleStorageService.deleteByTimeAndImportance(expireTime, MIN_IMPORTANCE_FOR_RETENTION);
            log.info("定时清理任务完成，共清理 {} 条低重要性过期数据", deletedCount);

        } catch (Exception e) {
            log.error("定时任务执行失败：分层清理过期新闻数据", e);
        }
    }

    /**
     * 手动触发：处理指定数据源的数据（采集即向量化）
     */
    public void processSpecificDataSource(FinancialArticleDSTypeEnum sourceType) {
        log.info("手动触发：处理数据源 [{}]", sourceType.getDesc());

        try {
            int savedCount = financialArticleDataService.processFinancialArticle(sourceType);
            log.info("手动触发完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发执行失败：处理数据源 [{}]", sourceType.getDesc(), e);
        }
    }

}

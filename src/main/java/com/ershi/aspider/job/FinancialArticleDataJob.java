package com.ershi.aspider.job;

import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.orchestration.service.FinancialArticleDataService;
import com.ershi.aspider.storage.elasticsearch.service.FinancialArticleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时收取财经数据
 *
 * @author Ershi-Gu.
 * @since 2025-11-12
 */
@Component
public class FinancialArticleDataJob {

    public static final Logger log = LoggerFactory.getLogger(FinancialArticleDataJob.class);

    /** 新闻数据保留天数 */
    private static final int RETENTION_DAYS = 30;

    private final FinancialArticleDataService financialArticleDataService;
    private final FinancialArticleStorageService financialArticleStorageService;

    public FinancialArticleDataJob(FinancialArticleDataService financialArticleDataService,
                                   FinancialArticleStorageService financialArticleStorageService) {
        this.financialArticleDataService = financialArticleDataService;
        this.financialArticleStorageService = financialArticleStorageService;
    }

    /**
     * 手动触发：持久化指定数据源的数据
     */
    public void processSpecificDataSource(FinancialArticleDSTypeEnum financialArticleDSTypeEnum) {
        log.info("手动触发：持久化数据源 [{}]", financialArticleDSTypeEnum.getDesc());

        try {
            int savedCount = financialArticleDataService.processFinancialArticleRawOnly(financialArticleDSTypeEnum);
            log.info("手动触发完成，成功保存 {} 条新数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发失败", e);
        }
    }

    /**
     * 定时任务：获取所有数据源数据并处理
     * 每小时执行一次（整点触发）
     */
    @Scheduled(cron = "0 0 0/3 * * ?")
    public void scheduledProcessAllDataSources() {
        log.info("定时任务启动：开始获取所有数据源数据");

        try {
            int savedCount = financialArticleDataService.processAllFinancialArticle();
            log.info("定时任务完成，成功保存 {} 条新数据", savedCount);

        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * 定时清理任务：每日凌晨2:00清理过期新闻数据
     * <p>
     * 清理规则：删除 publishTime 早于 (当前时间 - 30天) 的数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanExpiredData() {
        log.info("定时清理任务启动：开始清理超过 {} 天的过期新闻数据", RETENTION_DAYS);

        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(RETENTION_DAYS);
            long deletedCount = financialArticleStorageService.deleteByPublishTimeBefore(expireTime);
            log.info("定时清理任务完成，共清理 {} 条过期数据", deletedCount);

        } catch (Exception e) {
            log.error("定时清理任务执行失败", e);
        }
    }

    /**
     * 手动触发：清理过期新闻数据
     *
     * @param days 保留最近多少天的数据
     * @return 删除的数据条数
     */
    public long cleanExpiredData(int days) {
        log.info("手动触发：清理超过 {} 天的过期新闻数据", days);

        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
            long deletedCount = financialArticleStorageService.deleteByPublishTimeBefore(expireTime);
            log.info("手动清理完成，共清理 {} 条过期数据", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("手动清理失败", e);
            return 0;
        }
    }

}

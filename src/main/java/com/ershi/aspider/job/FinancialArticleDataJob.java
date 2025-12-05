package com.ershi.aspider.job;

import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.orchestration.service.FinancialArticleDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时收取财经数据
 *
 * @author Ershi-Gu.
 * @since 2025-11-12
 */
@Component
public class FinancialArticleDataJob {

    public static final Logger log = LoggerFactory.getLogger(FinancialArticleDataJob.class);

    private final FinancialArticleDataService financialArticleDataService;

    public FinancialArticleDataJob(FinancialArticleDataService financialArticleDataService) {
        this.financialArticleDataService = financialArticleDataService;
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

}

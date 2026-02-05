package com.ershi.aspider.data.job;

import com.ershi.aspider.data.datasource.domain.SectorTypeEnum;
import com.ershi.aspider.data.orchestration.service.SectorDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 板块数据定时采集任务
 * <p>
 * A股交易时间：9:30-11:30, 13:00-15:00
 * <p>
 * 采集策略：
 * 1. 资金流向 - 盘中实时采集：交易时段每30分钟采集一次，用于实时分析
 * 2. 资金流向 - 收盘后采集：15:30采集当日完整数据
 * 3. 板块行情 - 收盘后采集：15:35采集当日行情数据（日K数据）
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Component
public class SectorMoneyFlowDataJob {

    private static final Logger log = LoggerFactory.getLogger(SectorMoneyFlowDataJob.class);

    private final SectorDataService sectorDataService;

    public SectorMoneyFlowDataJob(SectorDataService sectorDataService) {
        this.sectorDataService = sectorDataService;
    }

    // ==================== 资金流向定时任务 ====================

    /**
     * 盘中实时采集：交易时段每30分钟执行
     * <p>
     * 上午场次：9:30, 10:00, 10:30, 11:00, 11:30
     * 下午场次：13:00, 13:30, 14:00, 14:30, 15:00
     * <p>
     * cron说明：0 0/30 9-11,13-15 * * MON-FRI
     * - 0 0/30：每30分钟（0分和30分）
     * - 9-11,13-15：上午9-11点，下午13-15点
     * - MON-FRI：周一至周五（排除周末）
     */
    @Scheduled(cron = "0 0/30 9-11,13-15 * * MON-FRI")
    public void scheduledRealtimeProcess() {
        log.info("定时任务启动：开始盘中实时采集板块资金流向数据");

        try {
            int savedCount = sectorDataService.processAllSectorMoneyFlow();
            log.info("定时任务完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("定时任务执行失败：盘中实时采集板块资金流向数据", e);
        }
    }

    /**
     * 收盘后采集资金流向：每日15:30执行
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void scheduledMoneyFlowAfterMarketClose() {
        log.info("定时任务启动：开始收盘采集当日完整板块资金流向数据");

        try {
            int savedCount = sectorDataService.processAllSectorMoneyFlow();
            log.info("定时任务完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("定时任务执行失败：收盘采集板块资金流向数据", e);
        }
    }

    // ==================== 板块行情定时任务 ====================

    /**
     * 收盘后采集板块行情：每日15:35执行
     * <p>
     * 板块行情为日K数据，只需收盘后采集一次
     */
    @Scheduled(cron = "0 35 15 * * MON-FRI")
    public void scheduledQuoteAfterMarketClose() {
        log.info("定时任务启动：开始收盘采集当日板块行情数据");

        try {
            int savedCount = sectorDataService.processAllSectorQuote();
            log.info("定时任务完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("定时任务执行失败：收盘采集板块行情数据", e);
        }
    }

    // ==================== 手动触发方法 ====================

    /**
     * 手动触发：采集所有板块资金流向数据
     */
    public void processAllSectorMoneyFlow() {
        log.info("手动触发：开始采集所有板块资金流向数据");

        try {
            int savedCount = sectorDataService.processAllSectorMoneyFlow();
            log.info("手动触发完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发执行失败：采集所有板块资金流向数据", e);
        }
    }

    /**
     * 手动触发：采集所有板块行情数据
     */
    public void processAllSectorQuote() {
        log.info("手动触发：开始采集所有板块行情数据");

        try {
            int savedCount = sectorDataService.processAllSectorQuote();
            log.info("手动触发完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发执行失败：采集所有板块行情数据", e);
        }
    }

    /**
     * 手动触发：采集所有板块数据（资金流向 + 行情）
     */
    public void processAllSectorData() {
        log.info("手动触发：开始采集所有板块数据");

        try {
            int savedCount = sectorDataService.processAllSectorData();
            log.info("手动触发完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发执行失败：采集所有板块数据", e);
        }
    }

    /**
     * 手动触发：采集指定板块类型的资金流向数据
     *
     * @param sectorType 板块类型
     */
    public void processSectorMoneyFlow(SectorTypeEnum sectorType) {
        log.info("手动触发：开始采集 [{}] 资金流向数据", sectorType.getDesc());

        try {
            int savedCount = sectorDataService.processSectorMoneyFlow(sectorType);
            log.info("手动触发完成，成功保存 {} 条数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发执行失败：采集 [{}] 资金流向数据", sectorType.getDesc(), e);
        }
    }
}
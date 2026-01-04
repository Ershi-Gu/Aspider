package com.ershi.aspider;

import com.ershi.aspider.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.datasource.domain.SectorTypeEnum;
import com.ershi.aspider.datasource.provider.EastMoneySectorMoneyFlowDS;
import com.ershi.aspider.job.SectorMoneyFlowDataJob;
import com.ershi.aspider.orchestration.service.SectorDataService;
import com.ershi.aspider.storage.elasticsearch.service.SectorMoneyFlowStorageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 板块资金流向测试类
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@SpringBootTest(classes = ASpiderApplication.class)
public class SectorMoneyFlowTest {

    @Resource
    private EastMoneySectorMoneyFlowDS sectorMoneyFlowDS;

    @Resource
    private SectorMoneyFlowStorageService storageService;

    @Resource
    private SectorDataService sectorDataService;

    @Resource
    private SectorMoneyFlowDataJob sectorMoneyFlowDataJob;

    /**
     * 测试数据源获取 - 行业板块
     */
    @Test
    public void testGetSectorMoneyFlow() {
        List<SectorMoneyFlow> data = sectorMoneyFlowDS.getSectorMoneyFlow(SectorTypeEnum.INDUSTRY);
        System.out.println("行业板块数量: " + data.size());

        if (!data.isEmpty()) {
            SectorMoneyFlow first = data.get(0);
            System.out.printf("示例: %s, 涨跌幅: %s%%, 主力净流入: %s%n",
                first.getSectorName(), first.getChangePercent(), first.getMainNetInflow());
        }
    }

    /**
     * 测试数据源获取 - 概念板块
     */
    @Test
    public void testGetConceptSectorMoneyFlow() {
        List<SectorMoneyFlow> data = sectorMoneyFlowDS.getSectorMoneyFlow(SectorTypeEnum.CONCEPT);
        System.out.println("概念板块数量: " + data.size());

        if (!data.isEmpty()) {
            SectorMoneyFlow first = data.get(0);
            System.out.printf("示例: %s, 涨跌幅: %s%%, 主力净流入: %s%n",
                first.getSectorName(), first.getChangePercent(), first.getMainNetInflow());
        }
    }

    /**
     * 测试存储到ES（需要先创建索引）
     */
    @Test
    public void testSaveToEs() {
        List<SectorMoneyFlow> data = sectorMoneyFlowDS.getAllSectorMoneyFlow();
        int count = storageService.batchSaveToEs(data);
        System.out.println("保存成功: " + count);
    }

    /**
     * 测试编排服务 - 处理所有板块并存储到ES
     * 注意：运行前请先创建ES索引
     */
    @Test
    public void testProcessAllSectorMoneyFlow() {
        int count = sectorDataService.processAllSectorMoneyFlow();
        System.out.println("成功保存数据条数: " + count);
    }

    /**
     * 测试定时任务手动触发
     * 注意：运行前请先创建ES索引
     */
    @Test
    public void testSectorMoneyFlowJob() {
        sectorMoneyFlowDataJob.processAllSectorMoneyFlow();
    }
}
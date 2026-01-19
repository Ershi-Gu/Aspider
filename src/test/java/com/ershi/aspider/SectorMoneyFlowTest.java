package com.ershi.aspider;

import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import com.ershi.aspider.data.datasource.domain.SectorTypeEnum;
import com.ershi.aspider.data.datasource.provider.EastMoneySectorMoneyFlowDS;
import com.ershi.aspider.data.datasource.provider.EastMoneySectorQuoteDS;
import com.ershi.aspider.data.job.SectorMoneyFlowDataJob;
import com.ershi.aspider.data.orchestration.service.SectorDataService;
import com.ershi.aspider.data.storage.elasticsearch.service.SectorMoneyFlowStorageService;
import com.ershi.aspider.data.storage.elasticsearch.service.SectorQuoteStorageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
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

    @Resource
    private EastMoneySectorQuoteDS sectorQuoteDS;

    @Resource
    private SectorQuoteStorageService quoteStorageService;

    // ==================== 资金流向测试 ====================

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

    // ==================== 板块行情测试 ====================

    /**
     * 测试板块行情数据源 - 行业板块
     */
    @Test
    public void testGetSectorQuote() {
        List<SectorQuote> data = sectorQuoteDS.getSectorQuote(SectorTypeEnum.INDUSTRY);
        System.out.println("行业板块行情数量: " + data.size());

        if (!data.isEmpty()) {
            SectorQuote first = data.get(0);
            System.out.printf("示例: %s, 收盘价: %s, 涨跌幅: %s%%, 成交额: %s%n",
                first.getSectorName(), first.getClosePrice(), first.getChangePercent(), first.getAmount());
        }
    }

    /**
     * 测试板块行情数据源 - 概念板块
     */
    @Test
    public void testGetConceptSectorQuote() {
        List<SectorQuote> data = sectorQuoteDS.getSectorQuote(SectorTypeEnum.CONCEPT);
        System.out.println("概念板块行情数量: " + data.size());

        if (!data.isEmpty()) {
            SectorQuote first = data.get(0);
            System.out.printf("示例: %s, 收盘价: %s, 涨跌幅: %s%%, 成交额: %s%n",
                first.getSectorName(), first.getClosePrice(), first.getChangePercent(), first.getAmount());
        }
    }

    /**
     * 测试板块行情保存到ES（需要先创建索引）
     */
    @Test
    public void testSaveQuoteToEs() {
        List<SectorQuote> data = sectorQuoteDS.getAllSectorQuote();
        int count = quoteStorageService.batchSaveToEs(data);
        System.out.println("行情数据保存成功: " + count);
    }

    /**
     * 测试编排服务 - 处理所有板块行情数据
     * 注意：运行前请先创建ES索引
     */
    @Test
    public void testProcessAllSectorQuote() {
        int count = sectorDataService.processAllSectorQuote();
        System.out.println("成功保存行情数据条数: " + count);
    }

    /**
     * 测试行情定时任务手动触发
     * 注意：运行前请先创建ES索引
     */
    @Test
    public void testSectorQuoteJob() {
        sectorMoneyFlowDataJob.processAllSectorQuote();
    }

    // ==================== 综合测试 ====================

    /**
     * 测试同时处理资金流向和行情数据
     * 注意：运行前请先创建所有ES索引
     */
    @Test
    public void testProcessAllSectorData() {
        int count = sectorDataService.processAllSectorData();
        System.out.println("成功保存全部数据条数: " + count);
    }

    // ==================== 新增检索方法测试 ====================

    /**
     * 测试查询主力净流入Top N板块
     */
    @Test
    public void testFindTopByMainNetInflow() {
        LocalDate today = LocalDate.now();
        List<SectorMoneyFlow> topSectors = storageService.findTopByMainNetInflow(today, 10);

        System.out.println("=== " + today + " 主力净流入Top10板块 ===");
        int rank = 1;
        for (SectorMoneyFlow sector : topSectors) {
            System.out.printf("  %2d. %-10s 主力净流入: %+.2f 亿  涨幅: %+.2f%%%n",
                rank++,
                sector.getSectorName(),
                sector.getMainNetInflow().doubleValue() / 100000000.0,
                sector.getChangePercent().doubleValue());
        }
    }

    /**
     * 测试查询涨幅Top N板块
     */
    @Test
    public void testFindTopByChangePercent() {
        LocalDate today = LocalDate.now();
        List<SectorMoneyFlow> topSectors = storageService.findTopByChangePercent(today, 10);

        System.out.println("=== " + today + " 涨幅Top10板块 ===");
        int rank = 1;
        for (SectorMoneyFlow sector : topSectors) {
            System.out.printf("  %2d. %-10s 涨幅: %+.2f%%  主力净流入: %+.2f 亿%n",
                rank++,
                sector.getSectorName(),
                sector.getChangePercent().doubleValue(),
                sector.getMainNetInflow().doubleValue() / 100000000.0);
        }
    }

    /**
     * 测试查询板块连续N日资金流向
     */
    @Test
    public void testFindRecentBySectorCode() {
        // 先查询今日涨幅第一的板块
        LocalDate today = LocalDate.now();
        List<SectorMoneyFlow> topSectors = storageService.findTopByChangePercent(today, 1);

        if (topSectors.isEmpty()) {
            System.out.println("今日无数据，尝试查询固定板块 BK0477(半导体)");
            List<SectorMoneyFlow> recentData = storageService.findRecentBySectorCode("BK0477", 5);
            printRecentData("BK0477", recentData);
            return;
        }

        SectorMoneyFlow topSector = topSectors.get(0);
        String sectorCode = topSector.getSectorCode();
        String sectorName = topSector.getSectorName();

        List<SectorMoneyFlow> recentData = storageService.findRecentBySectorCode(sectorCode, 5);
        printRecentData(sectorName + "(" + sectorCode + ")", recentData);
    }

    private void printRecentData(String sectorInfo, List<SectorMoneyFlow> recentData) {
        System.out.println("=== " + sectorInfo + " 近5日资金流向 ===");
        for (SectorMoneyFlow data : recentData) {
            System.out.printf("  %s: 涨幅 %+.2f%%  主力净流入 %+.2f 亿%n",
                data.getTradeDate(),
                data.getChangePercent().doubleValue(),
                data.getMainNetInflow().doubleValue() / 100000000.0);
        }

        // 计算连续流入天数
        int consecutiveDays = 0;
        for (SectorMoneyFlow data : recentData) {
            if (data.getMainNetInflow() != null && data.getMainNetInflow().compareTo(java.math.BigDecimal.ZERO) > 0) {
                consecutiveDays++;
            } else {
                break;
            }
        }
        System.out.println("  连续流入天数: " + consecutiveDays);
    }

    /**
     * 测试按板块名称查询
     */
    @Test
    public void testFindBySectorNameAndDate() {
        LocalDate today = LocalDate.now();

        // 测试几个常见板块
        String[] sectorNames = {"半导体", "人工智能", "新能源车"};

        for (String sectorName : sectorNames) {
            SectorMoneyFlow result = storageService.findBySectorNameAndDate(sectorName, today);

            if (result != null) {
                System.out.printf("板块[%s]: 涨幅 %+.2f%%  主力净流入 %+.2f 亿  领涨股 %s%n",
                    sectorName,
                    result.getChangePercent().doubleValue(),
                    result.getMainNetInflow().doubleValue() / 100000000.0,
                    result.getLeadStockName());
            } else {
                System.out.println("板块[" + sectorName + "]: 今日无数据");
            }
        }
    }

    /**
     * 测试计算资金流入排名
     */
    @Test
    public void testCalculateInflowRank() {
        LocalDate today = LocalDate.now();

        // 先获取一个板块的代码
        List<SectorMoneyFlow> topSectors = storageService.findTopByMainNetInflow(today, 3);
        long totalCount = storageService.countByTradeDate(today);

        System.out.println("=== 板块资金流入排名测试 ===");
        System.out.println("今日板块总数: " + totalCount);

        for (SectorMoneyFlow sector : topSectors) {
            int rank = storageService.calculateInflowRank(sector.getSectorCode(), today);
            System.out.printf("  %s: 排名 %d / %d  主力净流入 %+.2f 亿%n",
                sector.getSectorName(),
                rank,
                totalCount,
                sector.getMainNetInflow().doubleValue() / 100000000.0);
        }
    }

    /**
     * 测试统计当日板块数量
     */
    @Test
    public void testCountByTradeDate() {
        LocalDate today = LocalDate.now();
        long count = storageService.countByTradeDate(today);
        System.out.println("今日(" + today + ")板块数量: " + count);

        // 测试昨日
        LocalDate yesterday = today.minusDays(1);
        long yesterdayCount = storageService.countByTradeDate(yesterday);
        System.out.println("昨日(" + yesterday + ")板块数量: " + yesterdayCount);
    }
}
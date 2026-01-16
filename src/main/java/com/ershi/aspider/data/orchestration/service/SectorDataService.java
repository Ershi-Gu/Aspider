package com.ershi.aspider.data.orchestration.service;

import com.ershi.aspider.data.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.data.datasource.domain.SectorQuote;
import com.ershi.aspider.data.datasource.domain.SectorTypeEnum;
import com.ershi.aspider.data.datasource.provider.SectorMoneyFlowDataSource;
import com.ershi.aspider.data.datasource.provider.SectorQuoteDataSource;
import com.ershi.aspider.data.storage.elasticsearch.service.SectorMoneyFlowStorageService;
import com.ershi.aspider.data.storage.elasticsearch.service.SectorQuoteStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 板块数据编排服务
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Service
public class SectorDataService {

    private static final Logger log = LoggerFactory.getLogger(SectorDataService.class);

    private final SectorMoneyFlowDataSource sectorMoneyFlowDataSource;
    private final SectorMoneyFlowStorageService sectorMoneyFlowStorageService;
    private final SectorQuoteDataSource sectorQuoteDataSource;
    private final SectorQuoteStorageService sectorQuoteStorageService;

    public SectorDataService(SectorMoneyFlowDataSource sectorMoneyFlowDataSource,
                             SectorMoneyFlowStorageService sectorMoneyFlowStorageService,
                             SectorQuoteDataSource sectorQuoteDataSource,
                             SectorQuoteStorageService sectorQuoteStorageService) {
        this.sectorMoneyFlowDataSource = sectorMoneyFlowDataSource;
        this.sectorMoneyFlowStorageService = sectorMoneyFlowStorageService;
        this.sectorQuoteDataSource = sectorQuoteDataSource;
        this.sectorQuoteStorageService = sectorQuoteStorageService;
    }

    // ==================== 资金流向 ====================

    /**
     * 处理所有板块类型的资金流向数据
     *
     * @return 成功保存的数据条数
     */
    public int processAllSectorMoneyFlow() {
        log.info("========== 开始处理所有板块资金流向数据 ==========");

        List<SectorMoneyFlow> allData = sectorMoneyFlowDataSource.getAllSectorMoneyFlow();
        if (allData.isEmpty()) {
            log.warn("未获取到任何板块资金流向数据");
            return 0;
        }

        int successCount = sectorMoneyFlowStorageService.batchSaveToEs(allData);
        log.info("========== 板块资金流向数据处理完成，保存 {} 条数据 ==========", successCount);
        return successCount;
    }

    /**
     * 处理指定板块类型的资金流向数据
     *
     * @param sectorType 板块类型
     * @return 成功保存的数据条数
     */
    public int processSectorMoneyFlow(SectorTypeEnum sectorType) {
        log.info("========== 开始处理{}资金流向数据 ==========", sectorType.getDesc());

        List<SectorMoneyFlow> data = sectorMoneyFlowDataSource.getSectorMoneyFlow(sectorType);
        if (data.isEmpty()) {
            log.warn("未获取到{}资金流向数据", sectorType.getDesc());
            return 0;
        }

        int successCount = sectorMoneyFlowStorageService.batchSaveToEs(data);
        log.info("========== {}资金流向数据处理完成，保存 {} 条数据 ==========", sectorType.getDesc(), successCount);
        return successCount;
    }

    // ==================== 板块行情 ====================

    /**
     * 处理所有板块类型的行情数据
     *
     * @return 成功保存的数据条数
     */
    public int processAllSectorQuote() {
        log.info("========== 开始处理所有板块行情数据 ==========");

        List<SectorQuote> allData = sectorQuoteDataSource.getAllSectorQuote();
        if (allData.isEmpty()) {
            log.warn("未获取到任何板块行情数据");
            return 0;
        }

        int successCount = sectorQuoteStorageService.batchSaveToEs(allData);
        log.info("========== 板块行情数据处理完成，保存 {} 条数据 ==========", successCount);
        return successCount;
    }

    /**
     * 处理指定板块类型的行情数据
     *
     * @param sectorType 板块类型
     * @return 成功保存的数据条数
     */
    public int processSectorQuote(SectorTypeEnum sectorType) {
        log.info("========== 开始处理{}行情数据 ==========", sectorType.getDesc());

        List<SectorQuote> data = sectorQuoteDataSource.getSectorQuote(sectorType);
        if (data.isEmpty()) {
            log.warn("未获取到{}行情数据", sectorType.getDesc());
            return 0;
        }

        int successCount = sectorQuoteStorageService.batchSaveToEs(data);
        log.info("========== {}行情数据处理完成，保存 {} 条数据 ==========", sectorType.getDesc(), successCount);
        return successCount;
    }

    // ==================== 综合处理 ====================

    /**
     * 处理所有板块数据（资金流向 + 行情）
     *
     * @return 成功保存的总数据条数
     */
    public int processAllSectorData() {
        log.info("========== 开始处理所有板块数据 ==========");

        int moneyFlowCount = processAllSectorMoneyFlow();
        int quoteCount = processAllSectorQuote();

        int totalCount = moneyFlowCount + quoteCount;
        log.info("========== 所有板块数据处理完成，共保存 {} 条数据（资金流向: {}, 行情: {}）==========",
                 totalCount, moneyFlowCount, quoteCount);
        return totalCount;
    }
}
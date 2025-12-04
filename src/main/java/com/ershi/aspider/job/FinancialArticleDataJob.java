package com.ershi.aspider.job;

import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.orchestration.service.FinancialArticleDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 定时收取新闻数据，并向量化后存入es
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

}

package com.ershi.aspider.datasource.job;

import com.ershi.aspider.datasource.domain.NewsDataSourceTypeEnum;
import com.ershi.aspider.datasource.service.NewsDataService;
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
public class NewsDataJob {

    public static final Logger log = LoggerFactory.getLogger(NewsDataJob.class);

    private final NewsDataService newsDataService;

    public NewsDataJob(NewsDataService newsDataService) {
        this.newsDataService = newsDataService;
    }

    /**
     * 手动触发：持久化指定数据源的数据
     */
    public void processSpecificDataSource(NewsDataSourceTypeEnum newsDataSourceTypeEnum) {
        log.info("手动触发：持久化数据源 [{}]", newsDataSourceTypeEnum.getDesc());

        try {
            int savedCount = newsDataService.processNewsDataBySource(newsDataSourceTypeEnum);
            log.info("手动触发完成，成功保存 {} 条新数据", savedCount);

        } catch (Exception e) {
            log.error("手动触发失败", e);
        }
    }

}

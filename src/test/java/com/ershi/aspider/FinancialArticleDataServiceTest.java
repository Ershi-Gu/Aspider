package com.ershi.aspider;

import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.job.FinancialArticleDataJob;
import com.ershi.aspider.datasource.service.FinancialArticleDSFactory;
import com.ershi.aspider.datasource.provider.FinancialArticleDataSource;
import com.ershi.aspider.datasource.domain.FinancialArticle;
import com.ershi.aspider.orchestration.service.FinancialArticleDataService;
import com.ershi.aspider.storage.elasticsearch.service.FinancialArticleStorageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest(classes = ASpiderApplication.class)
public class FinancialArticleDataServiceTest {

    @Resource
    private FinancialArticleDSFactory financialArticleDSFactory;

    @Resource
    private FinancialArticleDataService financialArticleDataService;

    @Resource
    private FinancialArticleDataJob financialArticleDataJob;

    @Resource
    private FinancialArticleStorageService financialArticleStorageService;

    @Test
    public void testFinancialArticleSource() {
        FinancialArticleDataSource dataSource = financialArticleDSFactory.getDataSource(FinancialArticleDSTypeEnum.EAST_MONEY);
        List<FinancialArticle> financialArticle = dataSource.getFinancialArticle();
        System.out.println(financialArticle);
    }

    @Test
    public void testFinancialArticleJob() {
        financialArticleDataJob.processSpecificDataSource(FinancialArticleDSTypeEnum.EAST_MONEY);
    }

    /**
     * 测试清理过期新闻数据（通过Job手动触发）
     * 清理30天前的数据
     */
    @Test
    public void testCleanExpiredData() {
        long deletedCount = financialArticleDataJob.cleanExpiredData(1);
        System.out.println("清理过期数据条数: " + deletedCount);
    }

    /**
     * 测试清理过期新闻数据（直接调用Storage服务）
     * 可自定义清理时间
     */
    @Test
    public void testDeleteByPublishTimeBefore() {
        // 删除7天前的数据（测试用，可调整）
        LocalDateTime expireTime = LocalDateTime.now().minusDays(7);
        long deletedCount = financialArticleStorageService.deleteByPublishTimeBefore(expireTime);
        System.out.println("清理 " + expireTime + " 之前的数据，共删除: " + deletedCount + " 条");
    }
}

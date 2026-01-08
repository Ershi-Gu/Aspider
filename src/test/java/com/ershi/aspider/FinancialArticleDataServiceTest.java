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
        LocalDateTime expireTime = LocalDateTime.now().minusDays(1);
        long deletedCount = financialArticleStorageService.deleteByPublishTimeBefore(expireTime);
        System.out.println("清理 " + expireTime + " 之前的数据，共删除: " + deletedCount + " 条");
    }

    /**
     * 测试统计未向量化数据数量
     */
    @Test
    public void testCountUnprocessed() {
        long count = financialArticleStorageService.countUnprocessed();
        System.out.println("未向量化数据数量: " + count);
    }

    /**
     * 测试查询未向量化的数据
     */
    @Test
    public void testFindUnprocessed() {
        List<FinancialArticle> unprocessedList = financialArticleStorageService.findUnprocessed(100);
        System.out.println("查询到未向量化数据: " + unprocessedList.size() + " 条");
        for (FinancialArticle article : unprocessedList) {
            System.out.println("  - " + article.getTitle() + " | processed=" + article.getProcessed());
        }
    }

    /**
     * 测试按需向量化处理（单批次）
     * 处理指定数量的未向量化数据
     */
    @Test
    public void testProcessUnvectorizedData() {
        int batchSize = 5;  // 每批处理5条
        int processedCount = financialArticleDataService.processUnvectorizedData(batchSize);
        System.out.println("按需向量化处理完成，本批次处理: " + processedCount + " 条");
    }

    /**
     * 测试处理所有未向量化数据（循环处理直到全部完成）
     */
    @Test
    public void testProcessAllUnvectorizedData() {
        int batchSize = 10;  // 每批处理10条
        int totalProcessed = financialArticleDataService.processAllUnvectorizedData(batchSize);
        System.out.println("全部向量化处理完成，共处理: " + totalProcessed + " 条");
    }

    /**
     * 测试获取未向量化数据统计（通过编排服务）
     */
    @Test
    public void testCountUnvectorizedData() {
        long count = financialArticleDataService.countUnvectorizedData();
        System.out.println("未向量化数据数量（编排服务）: " + count);
    }
}

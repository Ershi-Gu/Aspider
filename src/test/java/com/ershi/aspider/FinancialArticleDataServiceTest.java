package com.ershi.aspider;

import com.ershi.aspider.data.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.data.job.FinancialArticleDataJob;
import com.ershi.aspider.data.datasource.service.FinancialArticleDSFactory;
import com.ershi.aspider.data.datasource.provider.FinancialArticleDataSource;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.orchestration.service.FinancialArticleDataService;
import com.ershi.aspider.data.storage.elasticsearch.service.FinancialArticleStorageService;
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

    /**
     * 测试处理指定数据源（采集即向量化）
     */
    @Test
    public void testProcessSpecificDataSource() {
        financialArticleDataJob.processSpecificDataSource(FinancialArticleDSTypeEnum.EAST_MONEY);
    }

    /**
     * 测试处理所有数据源（采集即向量化）
     */
    @Test
    public void testProcessAllFinancialArticle() {
        int savedCount = financialArticleDataService.processAllFinancialArticle();
        System.out.println("处理完成，保存数据条数: " + savedCount);
    }

    /**
     * 测试分层清理过期新闻数据
     * 清理90天前且重要性 < 3 的数据
     */
    @Test
    public void testDeleteByTimeAndImportance() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(1);
        int minImportance = 6;
        long deletedCount = financialArticleStorageService.deleteByTimeAndImportance(expireTime, minImportance);
        System.out.println("分层清理完成，删除低重要性数据: " + deletedCount + " 条");
    }

    /**
     * 测试查询最近N天的新闻数据
     */
    @Test
    public void testFindRecentByDays() {
        List<FinancialArticle> articles = financialArticleStorageService.findRecentByDays(7, 100);
        System.out.println("查询到最近7天数据: " + articles.size() + " 条");
        for (FinancialArticle article : articles) {
            System.out.println("  - " + article.getTitle() + " | importance=" + article.getImportance() + " | newsType=" + article.getNewsType());
        }
    }
}

package com.ershi.aspider;

import com.ershi.aspider.data.datasource.domain.FinancialArticleDSTypeEnum;
import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import com.ershi.aspider.data.job.FinancialArticleDataJob;
import com.ershi.aspider.data.datasource.service.FinancialArticleDSFactory;
import com.ershi.aspider.data.datasource.provider.FinancialArticleDataSource;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.embedding.EmbeddingExecutor;
import com.ershi.aspider.data.orchestration.service.FinancialArticleDataService;
import com.ershi.aspider.data.storage.elasticsearch.service.FinancialArticleStorageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    private EmbeddingExecutor embeddingExecutor;

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

    // ==================== 新增检索方法测试 ====================

    /**
     * 测试向量KNN语义检索
     * <p>
     * 流程：查询词 -> 向量化 -> KNN检索 -> 返回语义相关新闻
     */
    @Test
    public void testSearchByVector() {
        // 1. 将查询词向量化（Double -> Float 转换）
        String query = "电网";
        List<Double> doubleVector = embeddingExecutor.embedText(query);
        List<Float> queryVector = doubleVector.stream()
            .map(Double::floatValue)
            .collect(Collectors.toList());

        System.out.println("查询词: " + query);
        System.out.println("向量维度: " + queryVector.size());

        // 2. 执行KNN检索（最近7天，Top 10）
        List<FinancialArticle> result = financialArticleStorageService.searchByVector(queryVector, 10, 7);

        System.out.println("\n向量检索结果: " + result.size() + " 条");
        for (FinancialArticle article : result) {
            System.out.println("  - " + article.getTitle());
            System.out.println("    摘要: " + (article.getSummary() != null ?
                article.getSummary().substring(0, Math.min(50, article.getSummary().length())) + "..." : "无"));
        }
    }

    /**
     * 测试不限时间范围的向量检索
     */
    @Test
    public void testSearchByVectorNoTimeLimit() {
        String query = "人工智能";
        List<Double> doubleVector = embeddingExecutor.embedText(query);
        List<Float> queryVector = doubleVector.stream()
            .map(Double::floatValue)
            .collect(Collectors.toList());

        // days=0 表示不限制时间范围
        List<FinancialArticle> result = financialArticleStorageService.searchByVector(queryVector, 5, 0);

        System.out.println("查询词: " + query + " (不限时间)");
        System.out.println("检索结果: " + result.size() + " 条");
        for (FinancialArticle article : result) {
            System.out.println("  - " + article.getTitle() + " | " + article.getPublishTime());
        }
    }

    /**
     * 测试按新闻类型查询
     */
    @Test
    public void testFindByNewsTypeAndDays() {
        // 测试查询政策类新闻
        List<FinancialArticle> policyNews = financialArticleStorageService
            .findByNewsTypeAndDays(NewsTypeEnum.POLICY, 7, 20);

        System.out.println("政策类新闻(最近7天): " + policyNews.size() + " 条");
        for (FinancialArticle article : policyNews) {
            System.out.println("  - " + article.getTitle() + " | newsType=" + article.getNewsType());
        }

        // 测试查询行业新闻
        List<FinancialArticle> industryNews = financialArticleStorageService
            .findByNewsTypeAndDays(NewsTypeEnum.INDUSTRY, 7, 20);

        System.out.println("\n行业类新闻(最近7天): " + industryNews.size() + " 条");
        for (FinancialArticle article : industryNews) {
            System.out.println("  - " + article.getTitle() + " | newsType=" + article.getNewsType());
        }
    }

    /**
     * 测试按重要性查询
     */
    @Test
    public void testFindByImportanceAndDays() {
        // 查询重要新闻（importance >= 3）
        List<FinancialArticle> importantNews = financialArticleStorageService
            .findByImportanceAndDays(3, 7, 20);

        System.out.println("重要新闻(importance>=3, 最近7天): " + importantNews.size() + " 条");
        for (FinancialArticle article : importantNews) {
            System.out.println("  - [" + article.getImportance() + "] " + article.getTitle());
        }

        // 查询重大新闻（importance >= 4）
        List<FinancialArticle> majorNews = financialArticleStorageService
            .findByImportanceAndDays(4, 30, 10);

        System.out.println("\n重大新闻(importance>=4, 最近30天): " + majorNews.size() + " 条");
        for (FinancialArticle article : majorNews) {
            System.out.println("  - [" + article.getImportance() + "] " + article.getTitle());
        }
    }

    /**
     * 测试混合检索（向量 + 关键词）
     * <p>
     * 流程：查询词 -> 向量化 -> 混合检索（向量相似度 + 关键词匹配 + 业务规则加权）
     */
    @Test
    public void testHybridSearch() {
        // 1. 将查询词向量化
        String query = "面条";
        List<Double> doubleVector = embeddingExecutor.embedText(query);
        List<Float> queryVector = doubleVector.stream()
            .map(Double::floatValue)
            .collect(Collectors.toList());

        System.out.println("查询词: " + query);
        System.out.println("向量维度: " + queryVector.size());

        // 2. 执行混合检索（最近7天，Top 10）
        List<FinancialArticle> result = financialArticleStorageService.hybridSearch(query, queryVector, 10, 7);

        System.out.println("\n混合检索结果: " + result.size() + " 条");
        for (FinancialArticle article : result) {
            System.out.println("  - " + article.getTitle());
            System.out.println("    重要性: " + article.getImportance() + " | 类型: " + article.getNewsType());
            System.out.println("    摘要: " + (article.getSummary() != null ?
                article.getSummary().substring(0, Math.min(50, article.getSummary().length())) + "..." : "无"));
        }
    }

    /**
     * 测试混合检索（不限时间）
     */
    @Test
    public void testHybridSearchNoTimeLimit() {
        String query = "人工智能";
        List<Double> doubleVector = embeddingExecutor.embedText(query);
        List<Float> queryVector = doubleVector.stream()
            .map(Double::floatValue)
            .collect(Collectors.toList());

        // days=0 表示不限制时间范围
        List<FinancialArticle> result = financialArticleStorageService.hybridSearch(query, queryVector, 5, 0);

        System.out.println("查询词: " + query + " (不限时间)");
        System.out.println("混合检索结果: " + result.size() + " 条");
        for (FinancialArticle article : result) {
            System.out.println("  - " + article.getTitle() + " | " + article.getPublishTime());
        }
    }
}

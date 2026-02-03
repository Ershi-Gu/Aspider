package com.ershi.aspider.analysis.retriever;

import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.enums.AnalysisType;
import com.ershi.aspider.analysis.retriever.domain.NewsRetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievedArticle;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnalysisRetriever 集成测试
 * <p>
 * 真实调用 ES 和 Embedding 服务，验证完整检索链路。
 * 需要确保 ES 服务和配置正确。
 *
 * @author Test
 */
@SpringBootTest
@DisplayName("AnalysisRetriever 集成测试")
class AnalysisRetrieverTest {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRetrieverTest.class);

    @Autowired
    private AnalysisRetriever analysisRetriever;

    @Autowired
    private NewsRetriever newsRetriever;

    @Autowired
    private SectorDataRetriever sectorDataRetriever;

    // ==================== AnalysisRetriever 综合检索测试 ====================

    @Nested
    @DisplayName("综合检索测试")
    class RetrieveIntegrationTests {

        @Test
        @DisplayName("按板块名称检索 - 半导体板块")
        void testRetrieve_BySectorName_Semiconductor() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("有色金属")
                .tradeDate(LocalDate.now())
                .newsDays(7)
                .newsTopK(20)
                .trendDays(5)
                .build();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert & Log
            assertNotNull(result, "检索结果不应为空");
            assertNotNull(result.getQuery(), "查询参数不应为空");
            assertNotNull(result.getRetrievalTime(), "检索时间不应为空");

            // 打印新闻检索结果
            NewsRetrievalResult newsResult = result.getNewsResult();
            assertNotNull(newsResult, "新闻结果不应为空");
            log.info("========== 新闻检索结果 ==========");
            log.info("候选数量: {}, 过滤后数量: {}", newsResult.getTotalCandidates(), newsResult.getFilteredCount());
            printNewsArticles(newsResult.getArticles());

            // 打印板块数据结果
            SectorDataResult sectorResult = result.getSectorResult();
            assertNotNull(sectorResult, "板块数据不应为空");
            log.info("========== 板块数据结果 ==========");
            printSectorDataResult(sectorResult);
        }

        @Test
        @DisplayName("按板块名称检索 - 人工智能板块")
        void testRetrieve_BySectorName_AI() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("人工智能")
                .tradeDate(LocalDate.now())
                .newsDays(7)
                .newsTopK(15)
                .trendDays(5)
                .build();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert & Log
            assertNotNull(result);
            log.info("========== 人工智能板块检索 ==========");
            log.info("新闻数量: {}", result.getNewsResult().getFilteredCount());
            log.info("板块排名: {}/{}", result.getSectorResult().getInflowRank(), result.getSectorResult().getTotalSectors());
            printNewsArticles(result.getNewsResult().getArticles());
        }

        @Test
        @DisplayName("按板块名称检索 - 电网设备板块")
        void testRetrieve_BySectorName_PowerGrid() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("电网设备")
                .tradeDate(LocalDate.now())
                .newsDays(14)
                .newsTopK(20)
                .build();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert
            assertNotNull(result);
            log.info("========== 电网设备板块检索 ==========");
            log.info("新闻数量: {}, 趋势方向: {}",
                     result.getNewsResult().getFilteredCount(),
                     result.getSectorResult().getTrendIndicator().getDirection());
            printNewsArticles(result.getNewsResult().getArticles());
            printSectorDataResult(result.getSectorResult());
        }

        @Test
        @DisplayName("按板块代码检索")
        void testRetrieve_BySectorCode() {
            // Arrange - 使用板块代码 BK0477 (半导体概念)
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorCode("BK0477")
                .tradeDate(LocalDate.now())
                .trendDays(5)
                .build();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert
            assertNotNull(result);
            log.info("========== 板块代码检索 (BK0477) ==========");
            log.info("板块排名: {}/{}", result.getSectorResult().getInflowRank(), result.getSectorResult().getTotalSectors());
            if (result.getSectorResult().getTodayFlow() != null) {
                log.info("今日主力净流入: {}", result.getSectorResult().getTodayFlow().getMainNetInflow());
            }
        }

        @Test
        @DisplayName("检索计划构建")
        void testBuildPlan() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("新能源")
                .analysisType(AnalysisType.TOP_SECTORS)
                .build();

            // Act
            AnalysisRetriever.RetrievalPlan plan = analysisRetriever.buildPlan(query);

            // Assert
            assertNotNull(plan);
            assertNotNull(plan.getSteps());
            assertFalse(plan.getSteps().isEmpty());
            log.info("========== 检索计划 ==========");
            for (String step : plan.getSteps()) {
                log.info("步骤: {}", step);
            }
        }
    }

    // ==================== NewsRetriever 新闻检索测试 ====================

    @Nested
    @DisplayName("新闻检索测试")
    class NewsRetrieverTests {

        @Test
        @DisplayName("混合语义检索 - 半导体相关新闻")
        void testRetrieveBySectorName_Semiconductor() {
            // Act
            List<RetrievedArticle> articles = newsRetriever.retrieveBySectorName("半导体", 7, 20);

            // Assert & Log
            assertNotNull(articles);
            log.info("========== 半导体新闻混合检索 ==========");
            log.info("检索到 {} 条新闻", articles.size());
            printNewsArticles(articles);
        }

        @Test
        @DisplayName("混合语义检索 - 政策相关新闻")
        void testRetrieveBySectorName_Policy() {
            // Act
            List<RetrievedArticle> articles = newsRetriever.retrieveBySectorName("政策", 14, 15);

            // Assert & Log
            assertNotNull(articles);
            log.info("========== 政策相关新闻检索 ==========");
            log.info("检索到 {} 条新闻", articles.size());
            printNewsArticles(articles);
        }

        @Test
        @DisplayName("政策类新闻检索")
        void testRetrievePolicyNews() {
            // Act
            List<RetrievedArticle> articles = newsRetriever.retrievePolicyNews(7, 20);

            // Assert & Log
            assertNotNull(articles);
            log.info("========== 政策类新闻过滤 ==========");
            log.info("检索到 {} 条政策新闻", articles.size());
            printNewsArticles(articles);
        }

        @Test
        @DisplayName("高重要性新闻检索")
        void testRetrieveImportantNews() {
            // Act - 检索重要性 >= 3 的新闻
            List<RetrievedArticle> articles = newsRetriever.retrieveImportantNews(3, 7, 20);

            // Assert & Log
            assertNotNull(articles);
            log.info("========== 高重要性新闻 (importance >= 3) ==========");
            log.info("检索到 {} 条高重要性新闻", articles.size());
            printNewsArticles(articles);
        }

        @Test
        @DisplayName("完整新闻检索流程")
        void testRetrieve_FullPipeline() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("新能源车")
                .newsDays(10)
                .newsTopK(25)
                .build();

            // Act
            NewsRetrievalResult result = newsRetriever.retrieve(query);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getArticles());
            log.info("========== 新能源车完整新闻检索 ==========");
            log.info("候选数量: {}, 去重后: {}", result.getTotalCandidates(), result.getFilteredCount());
            printNewsArticles(result.getArticles());
        }
    }

    // ==================== SectorDataRetriever 板块数据检索测试 ====================

    @Nested
    @DisplayName("板块数据检索测试")
    class SectorDataRetrieverTests {

        @Test
        @DisplayName("按板块名称检索资金流向")
        void testRetrieve_BySectorName() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("半导体")
                .tradeDate(LocalDate.now())
                .trendDays(5)
                .build();

            // Act
            SectorDataResult result = sectorDataRetriever.retrieve(query);

            // Assert & Log
            assertNotNull(result);
            log.info("========== 半导体板块数据 ==========");
            printSectorDataResult(result);
        }

        @Test
        @DisplayName("按板块代码检索资金流向")
        void testRetrieve_BySectorCode() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorCode("BK0477")
                .trendDays(5)
                .build();

            // Act
            SectorDataResult result = sectorDataRetriever.retrieve(query);

            // Assert & Log
            assertNotNull(result);
            log.info("========== 板块代码 BK0477 资金流向 ==========");
            printSectorDataResult(result);
        }

        @Test
        @DisplayName("获取Top10板块 - 按主力净流入排序")
        void testGetTopSectors_ByInflow() {
            // Act
            var topSectors = sectorDataRetriever.getTopSectors(
                LocalDate.now(),
                10,
                SectorDataRetriever.TopSectorType.INFLOW
            );

            // Assert & Log
            assertNotNull(topSectors);
            log.info("========== Top10 主力净流入板块 ==========");
            for (int i = 0; i < topSectors.size(); i++) {
                var sector = topSectors.get(i);
                log.info("{}. {} ({}) - 主力净流入: {}, 涨跌幅: {}%",
                         i + 1,
                         sector.getSectorName(),
                         sector.getSectorCode(),
                         sector.getMainNetInflow(),
                         sector.getChangePercent());
            }
        }

        @Test
        @DisplayName("获取Top10板块 - 按涨跌幅排序")
        void testGetTopSectors_ByChange() {
            // Act
            var topSectors = sectorDataRetriever.getTopSectors(
                LocalDate.now(),
                10,
                SectorDataRetriever.TopSectorType.CHANGE
            );

            // Assert & Log
            assertNotNull(topSectors);
            log.info("========== Top10 涨幅板块 ==========");
            for (int i = 0; i < topSectors.size(); i++) {
                var sector = topSectors.get(i);
                log.info("{}. {} ({}) - 涨跌幅: {}%, 主力净流入: {}",
                         i + 1,
                         sector.getSectorName(),
                         sector.getSectorCode(),
                         sector.getChangePercent(),
                         sector.getMainNetInflow());
            }
        }

        @Test
        @DisplayName("趋势指标计算")
        void testCalculateTrend() {
            // Act - 计算某个板块的趋势
            var trend = sectorDataRetriever.calculateTrend("BK0477", 5);

            // Assert & Log
            assertNotNull(trend);
            log.info("========== 板块趋势指标 (BK0477) ==========");
            log.info("连续流入天数: {}", trend.getConsecutiveInflowDays());
            log.info("累计流入金额: {}", trend.getTotalInflow());
            log.info("平均涨跌幅: {}%", trend.getAvgChangePercent());
            log.info("趋势方向: {}", trend.getDirection());
        }
    }

    // ==================== 边界条件和异常场景测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空查询条件")
        void testRetrieve_WithEmptyQuery() {
            // Arrange
            AnalysisQuery query = new AnalysisQuery();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getNewsResult());
            assertNotNull(result.getSectorResult());
            log.info("========== 空查询结果 ==========");
            log.info("新闻数量: {}", result.getNewsResult().getFilteredCount());
        }

        @Test
        @DisplayName("null 查询条件")
        void testRetrieve_WithNullQuery() {
            // Act
            RetrievalResult result = analysisRetriever.retrieve(null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getQuery());
            log.info("========== null 查询结果 ==========");
            log.info("默认查询使用成功");
        }

        @Test
        @DisplayName("不存在的板块名称")
        void testRetrieve_WithNonExistentSector() {
            // Arrange
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("不存在的板块名称XYZ123")
                .build();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert - 不应抛出异常，应返回空或少量结果
            assertNotNull(result);
            log.info("========== 不存在板块查询 ==========");
            log.info("新闻数量: {}", result.getNewsResult().getFilteredCount());
            log.info("板块排名: {}", result.getSectorResult().getInflowRank());
        }

        @Test
        @DisplayName("历史日期查询")
        void testRetrieve_WithHistoricalDate() {
            // Arrange - 查询7天前的数据
            AnalysisQuery query = AnalysisQuery.builder()
                .sectorName("半导体")
                .tradeDate(LocalDate.now().minusDays(7))
                .build();

            // Act
            RetrievalResult result = analysisRetriever.retrieve(query);

            // Assert
            assertNotNull(result);
            log.info("========== 历史日期查询 (7天前) ==========");
            log.info("新闻数量: {}", result.getNewsResult().getFilteredCount());
            printSectorDataResult(result.getSectorResult());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 打印新闻文章列表
     */
    private void printNewsArticles(List<RetrievedArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            log.info("无新闻数据");
            return;
        }
        for (int i = 0; i < Math.min(articles.size(), 10); i++) {
            RetrievedArticle item = articles.get(i);
            var article = item.getArticle();
            log.info("{}. [{}] {} | 来源: {} | 评分: {} | 重要性: {}",
                     i + 1,
                     article.getNewsType() != null ? article.getNewsType() : "UNKNOWN",
                     truncate(article.getTitle(), 40),
                     item.getSource(),
                     String.format("%.2f", item.getRelevanceScore()),
                     article.getImportance());
        }
        if (articles.size() > 10) {
            log.info("... 还有 {} 条新闻未显示", articles.size() - 10);
        }
    }

    /**
     * 打印板块数据结果
     */
    private void printSectorDataResult(SectorDataResult result) {
        if (result == null) {
            log.info("无板块数据");
            return;
        }
        log.info("板块排名: {}/{}", result.getInflowRank(), result.getTotalSectors());

        if (result.getTodayFlow() != null) {
            var flow = result.getTodayFlow();
            log.info("今日资金流向: {} ({}) | 主力净流入: {} | 涨跌幅: {}%",
                     flow.getSectorName(),
                     flow.getSectorCode(),
                     flow.getMainNetInflow(),
                     flow.getChangePercent());
        }

        if (result.getTodayQuote() != null) {
            var quote = result.getTodayQuote();
            log.info("今日行情: 开盘: {} | 收盘: {} | 换手率: {}%",
                     quote.getOpenPrice(),
                     quote.getClosePrice(),
                     quote.getTurnoverRate());
        }

        var trend = result.getTrendIndicator();
        if (trend != null) {
            log.info("趋势指标: 连续流入{}天 | 累计: {} | 平均涨跌: {}% | 方向: {}",
                     trend.getConsecutiveInflowDays(),
                     trend.getTotalInflow(),
                     String.format("%.2f", trend.getAvgChangePercent()),
                     trend.getDirection());
        }

        if (result.getRecentFlows() != null && !result.getRecentFlows().isEmpty()) {
            log.info("近期资金流向 ({}天):", result.getRecentFlows().size());
            for (var flow : result.getRecentFlows()) {
                log.info("  {} | 主力净流入: {} | 涨跌幅: {}%",
                         flow.getTradeDate(),
                         flow.getMainNetInflow(),
                         flow.getChangePercent());
            }
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen - 3) + "...";
    }
}
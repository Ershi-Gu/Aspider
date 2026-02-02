package com.ershi.aspider.analysis.retriever;

import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.NewsRetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.RetrievalResult;
import com.ershi.aspider.analysis.retriever.domain.SectorDataResult;
import com.ershi.aspider.analysis.retriever.domain.TrendDirection;
import com.ershi.aspider.analysis.retriever.domain.TrendIndicator;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 分析检索门面
 * <p>
 * 统一入口，并行执行新闻检索和板块数据检索，合并结果返回。
 *
 * @author Ershi-Gu
 */
@Service
public class AnalysisRetriever {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRetriever.class);

    private final NewsRetriever newsRetriever;
    private final SectorDataRetriever sectorDataRetriever;
    private final Executor executor;

    public AnalysisRetriever(NewsRetriever newsRetriever,
                             SectorDataRetriever sectorDataRetriever,
                             Executor aspiderVirtualExecutor) {
        this.newsRetriever = newsRetriever;
        this.sectorDataRetriever = sectorDataRetriever;
        this.executor = aspiderVirtualExecutor;
    }

    /**
     * 统一检索入口
     * <p>
     * 并行执行新闻检索和板块数据检索，提高响应速度。
     *
     * @param query 分析查询请求
     * @return 综合检索结果
     */
    public RetrievalResult retrieve(AnalysisQuery query) {
        AnalysisQuery safeQuery = query != null ? query : new AnalysisQuery();
        log.info("开始综合检索，板块={}, 日期={}", safeQuery.getSectorName(), safeQuery.getTradeDate());

        long startTime = System.currentTimeMillis();

        // 并行执行检索
        CompletableFuture<NewsRetrievalResult> newsFuture = CompletableFuture
            .supplyAsync(() -> newsRetriever.retrieve(safeQuery), executor)
            .exceptionally(ex -> {
                log.error("新闻检索失败", ex);
                return emptyNewsResult();
            });

        CompletableFuture<SectorDataResult> sectorFuture = CompletableFuture
            .supplyAsync(() -> sectorDataRetriever.retrieve(safeQuery), executor)
            .exceptionally(ex -> {
                log.error("板块数据检索失败", ex);
                return emptySectorResult();
            });

        // 等待结果
        RetrievalResult result = new RetrievalResult();
        result.setQuery(safeQuery);
        result.setNewsResult(newsFuture.join());
        result.setSectorResult(sectorFuture.join());
        result.setRetrievalTime(LocalDateTime.now());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("综合检索完成，耗时={}ms，新闻={}条，排名={}/{}",
                 elapsed,
                 result.getNewsResult().getFilteredCount(),
                 result.getSectorResult().getInflowRank(),
                 result.getSectorResult().getTotalSectors());

        return result;
    }

    /**
     * 构建检索计划（用于调试和优化）
     */
    public RetrievalPlan buildPlan(AnalysisQuery query) {
        RetrievalPlan plan = new RetrievalPlan();
        plan.setQuery(query);
        plan.setPlanTime(LocalDateTime.now());
        plan.setSteps(List.of(
            "1. NewsRetriever: 混合语义检索 + 政策新闻 + 重要新闻",
            "2. SectorDataRetriever: 资金流向 + 行情数据 + 趋势指标"
        ));
        return plan;
    }

    private NewsRetrievalResult emptyNewsResult() {
        NewsRetrievalResult result = new NewsRetrievalResult();
        result.setArticles(Collections.emptyList());
        result.setTotalCandidates(0);
        result.setFilteredCount(0);
        return result;
    }

    private SectorDataResult emptySectorResult() {
        SectorDataResult result = new SectorDataResult();
        result.setRecentFlows(Collections.emptyList());
        result.setInflowRank(-1);
        result.setTotalSectors(0);
        result.setTrendIndicator(emptyTrend());
        return result;
    }

    private TrendIndicator emptyTrend() {
        TrendIndicator indicator = new TrendIndicator();
        indicator.setConsecutiveInflowDays(0);
        indicator.setTotalInflow(0);
        indicator.setAvgChangePercent(0);
        indicator.setDirection(TrendDirection.NEUTRAL);
        return indicator;
    }

    /** 检索计划（用于调试） */
    @Data
    public static class RetrievalPlan {
        private AnalysisQuery query;
        private LocalDateTime planTime;
        private List<String> steps;
    }
}

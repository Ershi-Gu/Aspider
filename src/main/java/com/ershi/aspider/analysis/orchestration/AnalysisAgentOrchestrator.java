package com.ershi.aspider.analysis.orchestration;

import com.ershi.aspider.analysis.agent.core.*;
import com.ershi.aspider.analysis.agent.domain.*;
import com.ershi.aspider.analysis.report.ReportGeneratorService;
import com.ershi.aspider.analysis.report.domain.AnalysisReport;
import com.ershi.aspider.analysis.retriever.AnalysisRetriever;
import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import com.ershi.aspider.analysis.retriever.domain.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 分析Agent编排器
 *
 * 负责整体分析流程的编排： <br>
 * 1. 数据检索  <br>
 * 2. 并行执行三个子Agent <br>
 * 3. SynthesisAgent综合研判 <br>
 * 4. 报告组装
 *
 * @author Ershi-Gu
 */
@Service
@Slf4j
public class AnalysisAgentOrchestrator {

    private final AnalysisRetriever retriever;
    private final PolicyAgent policyAgent;
    private final SectorAgent sectorAgent;
    private final TrendAgent trendAgent;
    private final SynthesisAgent synthesisAgent;
    private final ReportGeneratorService reportGenerator;
    private final Executor executor;

    public AnalysisAgentOrchestrator(AnalysisRetriever retriever,
                                      PolicyAgent policyAgent,
                                      SectorAgent sectorAgent,
                                      TrendAgent trendAgent,
                                      SynthesisAgent synthesisAgent,
                                      ReportGeneratorService reportGenerator,
                                      Executor aspiderVirtualExecutor) {
        this.retriever = retriever;
        this.policyAgent = policyAgent;
        this.sectorAgent = sectorAgent;
        this.trendAgent = trendAgent;
        this.synthesisAgent = synthesisAgent;
        this.reportGenerator = reportGenerator;
        this.executor = aspiderVirtualExecutor;
    }

    /**
     * 执行板块分析（完整流程）
     *
     * @param query 分析查询请求
     * @return 完整分析报告
     */
    public AnalysisReport analyze(AnalysisQuery query) {
        log.info("开始板块分析，板块={}，日期={}", query.getSectorName(), query.getTradeDate());
        long startTime = System.currentTimeMillis();

        // 1. 数据检索
        RetrievalResult retrieval = retriever.retrieve(query);
        AgentContext context = AgentContext.builder()
            .retrievalResult(retrieval)
            .query(query)
            .analysisTime(LocalDateTime.now())
            .build();

        // 2. 第一层：并行执行三个子Agent
        CompletableFuture<PolicyImpact> policyFuture = CompletableFuture
            .supplyAsync(() -> policyAgent.analyze(context), executor)
            .exceptionally(ex -> {
                log.error("PolicyAgent执行失败", ex);
                return PolicyImpact.empty("execution_error");
            });

        CompletableFuture<SectorHeat> sectorFuture = CompletableFuture
            .supplyAsync(() -> sectorAgent.analyze(context), executor)
            .exceptionally(ex -> {
                log.error("SectorAgent执行失败", ex);
                return SectorHeat.empty("execution_error");
            });

        CompletableFuture<TrendSignal> trendFuture = CompletableFuture
            .supplyAsync(() -> trendAgent.analyze(context), executor)
            .exceptionally(ex -> {
                log.error("TrendAgent执行失败", ex);
                return TrendSignal.empty("execution_error");
            });

        // 等待所有子Agent完成
        PolicyImpact policy = policyFuture.join();
        SectorHeat sector = sectorFuture.join();
        TrendSignal trend = trendFuture.join();

        log.info("子Agent分析完成，消息面={}，资金面={}，趋势={}",
                 policy.getSignal(), sector.getCapitalSignal(), trend.getSignal());

        // 3. 第二层：SynthesisAgent综合研判
        SynthesisInput synthesisInput = SynthesisInput.builder()
            .retrievalResult(retrieval)
            .policyImpact(policy)
            .sectorHeat(sector)
            .trendSignal(trend)
            .query(query)
            .build();

        SynthesisResult synthesis = synthesisAgent.analyze(synthesisInput);

        // 4. 报告组装
        AnalysisReport report = reportGenerator.generate(
            query, retrieval, policy, sector, trend, synthesis
        );

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("板块分析完成，板块={}，评级={}，评分={}，LLM生成={}，耗时={}ms",
                 query.getSectorName(),
                 synthesis.getOverallRating(),
                 synthesis.getOverallScore(),
                 synthesis.isLlmGenerated(),
                 elapsed);

        return report;
    }

    /**
     * 仅执行检索和子Agent分析（不调用LLM）
     * 用于调试或在LLM不可用时使用
     *
     * @param query 分析查询请求
     * @return 综合研判结果（规则合成）
     */
    public SynthesisResult analyzeWithoutLlm(AnalysisQuery query) {
        log.info("开始板块分析（无LLM模式），板块={}", query.getSectorName());

        // 1. 数据检索
        RetrievalResult retrieval = retriever.retrieve(query);
        AgentContext context = AgentContext.builder()
            .retrievalResult(retrieval)
            .query(query)
            .analysisTime(LocalDateTime.now())
            .build();

        // 2. 串行执行三个子Agent（简化版）
        PolicyImpact policy = policyAgent.analyze(context);
        SectorHeat sector = sectorAgent.analyze(context);
        TrendSignal trend = trendAgent.analyze(context);

        // 3. 规则合成
        return SynthesisResult.fallback(policy, sector, trend);
    }
}

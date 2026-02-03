package com.ershi.aspider.analysis.orchestration;

import com.ershi.aspider.analysis.report.domain.AnalysisReport;
import com.ershi.aspider.analysis.retriever.domain.AnalysisQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分析编排器集成测试
 *
 * @author Ershi-Gu
 */
@SpringBootTest
class AnalysisAgentOrchestratorTest {

    @Autowired
    private AnalysisAgentOrchestrator orchestrator;

    @Test
    void testAnalyze_semiconductor() {
        // 构建查询
        AnalysisQuery query = new AnalysisQuery();
        query.setSectorName("半导体");
        query.setTradeDate(LocalDate.now());

        // 执行分析
        AnalysisReport report = orchestrator.analyze(query);

        // 验证结果
        assertNotNull(report, "报告不应为空");
        assertNotNull(report.getSignalCard(), "信号卡片不应为空");
        assertNotNull(report.getSignalCard().getOverallRating(), "综合评级不应为空");
        assertNotNull(report.getDashboard(), "数据看板不应为空");
        assertNotNull(report.getDetailAnalysis(), "详细分析不应为空");
        assertNotNull(report.getActionReference(), "操作参考不应为空");

        // 输出结果
        System.out.println("=== 分析报告 ===");
        System.out.println("板块: " + report.getSignalCard().getSectorName());
        System.out.println("综合评级: " + report.getSignalCard().getOverallRating());
        System.out.println("综合评分: " + report.getSignalCard().getOverallScore());
        System.out.println("一句话总结: " + report.getSignalCard().getSummary());
        System.out.println();
        System.out.println("四维信号灯:");
        System.out.println("  消息面: " + report.getSignalCard().getDimensionSignals().getNews());
        System.out.println("  资金面: " + report.getSignalCard().getDimensionSignals().getCapital());
        System.out.println("  技术面: " + report.getSignalCard().getDimensionSignals().getTechnical());
        System.out.println("  情绪面: " + report.getSignalCard().getDimensionSignals().getSentiment());
    }

    @Test
    void testAnalyzeWithoutLlm() {
        // 构建查询
        AnalysisQuery query = new AnalysisQuery();
        query.setSectorName("新能源");
        query.setTradeDate(LocalDate.now());

        // 执行无LLM分析
        var synthesis = orchestrator.analyzeWithoutLlm(query);

        // 验证结果
        assertNotNull(synthesis, "综合结果不应为空");
        assertFalse(synthesis.isLlmGenerated(), "应为规则生成");
        assertNotNull(synthesis.getOverallRating(), "评级不应为空");

        System.out.println("=== 无LLM模式分析结果 ===");
        System.out.println("综合评级: " + synthesis.getOverallRating());
        System.out.println("综合评分: " + synthesis.getOverallScore());
        System.out.println("总结: " + synthesis.getSummary());
        System.out.println("LLM生成: " + synthesis.isLlmGenerated());
    }
}

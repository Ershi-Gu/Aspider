package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析状态
 *
 * @author Ershi-Gu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisStatus {

    /** 降级消息：非AI分析 */
    public static final String MSG_NON_AI = "当前使用规则分析，非AI分析结果，可能存在不准确";

    /** 降级消息：LLM调用失败 */
    public static final String MSG_LLM_FALLBACK = "LLM服务暂时不可用，已降级为规则分析";

    /** 降级消息：Prompt模板缺失 */
    public static final String MSG_PROMPT_MISSING = "Prompt模板缺失，已降级为规则分析";

    /** 降级消息：LLM响应解析失败 */
    public static final String MSG_LLM_PARSE_ERROR = "LLM响应解析失败，已降级为规则分析";

    private StatusType type;
    private String message;

    public static AnalysisStatus normal() {
        return new AnalysisStatus(StatusType.NORMAL, null);
    }

    public static AnalysisStatus degraded(String reason) {
        return new AnalysisStatus(StatusType.DEGRADED, reason);
    }

    public static AnalysisStatus failed(String reason) {
        return new AnalysisStatus(StatusType.FAILED, reason);
    }

    /**
     * 创建非AI分析降级状态
     */
    public static AnalysisStatus degradedNonAi() {
        return new AnalysisStatus(StatusType.DEGRADED, MSG_NON_AI);
    }

    /**
     * 判断是否为降级状态
     */
    public boolean isDegraded() {
        return type == StatusType.DEGRADED;
    }
}

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
}

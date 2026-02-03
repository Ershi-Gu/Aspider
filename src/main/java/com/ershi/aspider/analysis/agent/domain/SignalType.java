package com.ershi.aspider.analysis.agent.domain;

import lombok.Getter;

/**
 * 单维度信号类型
 *
 * @author Ershi-Gu
 */
@Getter
public enum SignalType {

    POSITIVE("利好"),
    NEGATIVE("利空"),
    NEUTRAL("中性");

    private final String label;

    SignalType(String label) {
        this.label = label;
    }
}

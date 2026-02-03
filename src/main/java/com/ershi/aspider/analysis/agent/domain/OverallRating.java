package com.ershi.aspider.analysis.agent.domain;

import lombok.Getter;

/**
 * 综合评级
 *
 * @author Ershi-Gu
 */
@Getter
public enum OverallRating {

    STRONG_BULLISH("强势", 5),
    BULLISH("偏多", 4),
    NEUTRAL("中性", 3),
    BEARISH("偏空", 2),
    STRONG_BEARISH("弱势", 1);

    private final String label;
    private final int score;

    OverallRating(String label, int score) {
        this.label = label;
        this.score = score;
    }
}

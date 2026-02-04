package com.ershi.aspider.data.datasource.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 摘要质量等级枚举
 * <p>
 * 根据质量评分划分等级，用于决定是否触发LLM摘要重生成：
 * <ul>
 *   <li><b>HIGH</b>：高质量，无需处理</li>
 *   <li><b>MEDIUM</b>：中等质量，高价值文章时触发LLM</li>
 *   <li><b>LOW</b>：低质量，强制触发LLM</li>
 * </ul>
 *
 * @author Ershi-Gu
 */
@Getter
@AllArgsConstructor
public enum SummaryQualityLevel {

    HIGH("HIGH", "高质量"),

    MEDIUM("MEDIUM", "中等质量"),

    LOW("LOW", "低质量");

    private final String code;

    private final String description;

    /**
     * 根据质量评分判定等级
     *
     * @param score              质量评分 0-100
     * @param highThreshold      高质量阈值
     * @param lowThreshold       低质量阈值
     * @return 质量等级
     */
    public static SummaryQualityLevel fromScore(int score, int highThreshold, int lowThreshold) {
        if (score >= highThreshold) {
            return HIGH;
        }
        if (score < lowThreshold) {
            return LOW;
        }
        return MEDIUM;
    }
}

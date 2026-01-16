package com.ershi.aspider.data.processor.scorer.strategy;

import lombok.Getter;

/**
 * 评分策略类型枚举
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
@Getter
public enum ScoreStrategyType {

    /**
     * 基于规则的关键词匹配评分
     */
    RULE("rule"),

    /**
     * 基于LLM的语义理解评分
     */
    LLM("llm");

    private final String code;

    ScoreStrategyType(String code) {
        this.code = code;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 策略代码
     * @return 策略类型枚举，找不到返回null
     */
    public static ScoreStrategyType fromCode(String code) {
        for (ScoreStrategyType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}

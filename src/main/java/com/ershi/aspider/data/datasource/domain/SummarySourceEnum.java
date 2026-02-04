package com.ershi.aspider.data.datasource.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 摘要来源枚举
 * <p>
 * 标识文章摘要的生成方式，便于质量追溯和数据分析：
 * <ul>
 *   <li><b>RAW</b>：数据源自带摘要</li>
 *   <li><b>LLM</b>：采集阶段由LLM生成</li>
 *   <li><b>EXTRACTED</b>：从正文抽取</li>
 *   <li><b>TRUNCATED</b>：正文截取</li>
 *   <li><b>ANALYSIS_LLM</b>：分析阶段兜底由LLM生成</li>
 * </ul>
 *
 * @author Ershi-Gu
 */
@Getter
@AllArgsConstructor
public enum SummarySourceEnum {

    RAW("RAW", "数据源自带"),

    LLM("LLM", "采集阶段LLM生成"),

    EXTRACTED("EXTRACTED", "正文抽取"),

    TRUNCATED("TRUNCATED", "正文截取"),

    ANALYSIS_LLM("ANALYSIS_LLM", "分析阶段LLM兜底");

    private final String code;

    private final String description;

    public static SummarySourceEnum fromCode(String code) {
        for (SummarySourceEnum source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        return RAW;
    }
}

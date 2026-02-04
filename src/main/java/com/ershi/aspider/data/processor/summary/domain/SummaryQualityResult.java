package com.ershi.aspider.data.processor.summary.domain;

import com.ershi.aspider.data.datasource.domain.SummaryQualityLevel;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 摘要质量评分结果
 *
 * @author Ershi-Gu
 */
@Data
@Builder
public class SummaryQualityResult {

    /** 质量评分 0-100 */
    private int score;

    /** 质量等级 */
    private SummaryQualityLevel level;

    /** 扣分原因列表 */
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    /** 是否需要LLM重新生成 */
    private boolean needLlm;

    /**
     * 创建默认高质量结果
     */
    public static SummaryQualityResult highQuality() {
        return SummaryQualityResult.builder()
            .score(100)
            .level(SummaryQualityLevel.HIGH)
            .needLlm(false)
            .build();
    }

    /**
     * 创建空摘要结果（最低质量）
     */
    public static SummaryQualityResult emptySummary() {
        List<String> reasons = new ArrayList<>();
        reasons.add("摘要为空");
        return SummaryQualityResult.builder()
            .score(0)
            .level(SummaryQualityLevel.LOW)
            .reasons(reasons)
            .needLlm(true)
            .build();
    }
}

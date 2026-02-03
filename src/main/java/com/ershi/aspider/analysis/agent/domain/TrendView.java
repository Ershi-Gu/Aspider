package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势研判视图
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendView {

    /** 观点：震荡偏强/谨慎乐观等 */
    private String viewpoint;

    /** 依据 */
    private String basis;
}

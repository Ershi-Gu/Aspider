package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * L4 操作参考
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionReference {

    /** 固定免责声明 */
    public static final String DISCLAIMER =
        "以上仅为数据分析结果，不构成投资建议。投资有风险，入市需谨慎。";

    /** 支撑位 */
    private BigDecimal supportLevel;

    /** 压力位 */
    private BigDecimal resistanceLevel;

    /** 风险提示列表 */
    private List<String> riskWarnings;

    /**
     * 获取免责声明
     */
    public String getDisclaimer() {
        return DISCLAIMER;
    }
}

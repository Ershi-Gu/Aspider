package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资金动向数据
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalMovement {

    /** 主力净流入（元） */
    private BigDecimal mainNetInflow;

    /** 连续流入天数 */
    private int consecutiveInflowDays;

    /** 超大单占比（%） */
    private BigDecimal superLargeRatio;
}

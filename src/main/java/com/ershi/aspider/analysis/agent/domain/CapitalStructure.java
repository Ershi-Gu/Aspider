package com.ershi.aspider.analysis.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资金结构详情
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalStructure {

    /** 超大单净流入（元） */
    private BigDecimal superLargeInflow;

    /** 大单净流入（元） */
    private BigDecimal largeInflow;

    /** 中单净流入（元） */
    private BigDecimal mediumInflow;

    /** 小单净流入（元） */
    private BigDecimal smallInflow;
}

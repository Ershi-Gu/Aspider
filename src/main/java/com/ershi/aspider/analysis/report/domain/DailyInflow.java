package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单日资金流入数据
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyInflow {

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 主力净流入（元） */
    private BigDecimal mainNetInflow;
}

package com.ershi.aspider.analysis.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单日涨跌幅数据
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChange {

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 涨跌幅（%） */
    private BigDecimal changePercent;
}

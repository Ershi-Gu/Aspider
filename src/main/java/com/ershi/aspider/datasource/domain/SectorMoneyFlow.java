package com.ershi.aspider.datasource.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 板块资金流向数据
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Data
public class SectorMoneyFlow {

    /** 唯一标识 (sectorCode + tradeDate 的MD5) */
    private String uniqueId;

    /** 板块代码 (如 BK0477) */
    private String sectorCode;

    /** 板块名称 (如 半导体) */
    private String sectorName;

    /** 板块类型 (INDUSTRY/CONCEPT) */
    private String sectorType;

    /** 交易日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;

    /** 涨跌幅(%) */
    private BigDecimal changePercent;

    /** 主力净流入(元) */
    private BigDecimal mainNetInflow;

    /** 主力净流入占比(%) */
    private BigDecimal mainNetInflowRatio;

    /** 超大单净流入(元) */
    private BigDecimal superLargeInflow;

    /** 超大单净流入占比(%) */
    private BigDecimal superLargeInflowRatio;

    /** 大单净流入(元) */
    private BigDecimal largeInflow;

    /** 大单净流入占比(%) */
    private BigDecimal largeInflowRatio;

    /** 中单净流入(元) */
    private BigDecimal mediumInflow;

    /** 中单净流入占比(%) */
    private BigDecimal mediumInflowRatio;

    /** 小单净流入(元) */
    private BigDecimal smallInflow;

    /** 小单净流入占比(%) */
    private BigDecimal smallInflowRatio;

    /** 领涨股票代码 */
    private String leadStock;

    /** 领涨股票名称 */
    private String leadStockName;

    /** 爬取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime crawlTime = LocalDateTime.now();

    /**
     * 生成唯一ID（基于 sectorCode + tradeDate）
     */
    public void generateUniqueId() {
        if (sectorCode != null && tradeDate != null) {
            String source = sectorCode + tradeDate.toString();
            this.uniqueId = DigestUtils.md5DigestAsHex(source.getBytes());
        }
    }
}
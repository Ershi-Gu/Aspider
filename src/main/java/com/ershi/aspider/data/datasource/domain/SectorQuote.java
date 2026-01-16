package com.ershi.aspider.data.datasource.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 板块行情数据
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
@Data
public class SectorQuote {

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

    /** 开盘价 */
    private BigDecimal openPrice;

    /** 收盘价/最新价 */
    private BigDecimal closePrice;

    /** 最高价 */
    private BigDecimal highPrice;

    /** 最低价 */
    private BigDecimal lowPrice;

    /** 涨跌幅(%) */
    private BigDecimal changePercent;

    /** 涨跌额 */
    private BigDecimal changeAmount;

    /** 换手率(%) */
    private BigDecimal turnoverRate;

    /** 成交额(元) */
    private BigDecimal amount;

    /** 成交量(手) */
    private BigDecimal volume;

    /** 振幅(%) */
    private BigDecimal amplitude;

    /** 成分股数量 */
    private Integer companyCount;

    /** 上涨家数 */
    private Integer riseCount;

    /** 下跌家数 */
    private Integer fallCount;

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
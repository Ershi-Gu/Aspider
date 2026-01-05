package com.ershi.aspider.datasource.provider;

import com.ershi.aspider.datasource.domain.SectorQuote;
import com.ershi.aspider.datasource.domain.SectorTypeEnum;

import java.util.List;

/**
 * 板块行情数据源接口
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
public interface SectorQuoteDataSource {

    /**
     * 获取板块行情数据
     *
     * @param sectorType 板块类型（行业/概念）
     * @return {@link List }<{@link SectorQuote }>
     */
    List<SectorQuote> getSectorQuote(SectorTypeEnum sectorType);

    /**
     * 获取所有板块类型的行情数据
     *
     * @return {@link List }<{@link SectorQuote }>
     */
    List<SectorQuote> getAllSectorQuote();
}
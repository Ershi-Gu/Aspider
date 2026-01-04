package com.ershi.aspider.datasource.provider;

import com.ershi.aspider.datasource.domain.SectorMoneyFlow;
import com.ershi.aspider.datasource.domain.SectorTypeEnum;

import java.util.List;

/**
 * 板块资金流向数据源接口
 *
 * @author Ershi-Gu.
 * @since 2025-12-25
 */
public interface SectorMoneyFlowDataSource {

    /**
     * 获取板块资金流向数据
     *
     * @param sectorType 板块类型（行业/概念）
     * @return {@link List }<{@link SectorMoneyFlow }>
     */
    List<SectorMoneyFlow> getSectorMoneyFlow(SectorTypeEnum sectorType);

    /**
     * 获取所有板块类型的资金流向数据
     *
     * @return {@link List }<{@link SectorMoneyFlow }>
     */
    List<SectorMoneyFlow> getAllSectorMoneyFlow();
}
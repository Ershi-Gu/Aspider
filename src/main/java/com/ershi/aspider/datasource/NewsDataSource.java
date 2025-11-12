package com.ershi.aspider.datasource;

import com.ershi.aspider.datasource.domain.entity.NewsDataItem;
import com.ershi.aspider.datasource.domain.enums.DataSourceTypeEnum;

import java.util.List;

/**
 * 分析数据数据源接口，可实现该接口拓展数据源。
 *
 * @author Ershi-Gu.
 * @since 2025-11-10
 */
public interface NewsDataSource {

    /**
     * 数据源类型
     *
     * @return {@link DataSourceTypeEnum }
     */
    DataSourceTypeEnum getDataSourceType();

    /**
     * 获取新闻数据
     *
     * @return {@link List }<{@link NewsDataItem }>
     */
    List<NewsDataItem> getNewsData();
}

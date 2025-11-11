package com.ershi.aspider.datasource;

import com.ershi.aspider.datasource.entity.NewsDataItem;

import java.util.List;

/**
 * 分析数据数据源接口，可实现该接口拓展数据源。
 *
 * @author Ershi-Gu.
 * @since 2025-11-10
 */
public interface NewsDataSource {

    /**
     * 获取新闻数据
     *
     * @return {@link List }<{@link NewsDataItem }>
     */
    List<NewsDataItem> getNewsData();
}

package com.ershi.aspider.datasource.service;

import com.ershi.aspider.datasource.NewsDataSource;
import com.ershi.aspider.datasource.domain.NewsDataSourceTypeEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源执行器，用于调用各个数据源获取政策新闻数据，并执行系列持久化操作。
 *
 * @author Ershi-Gu.
 * @since 2025-11-11
 */
@Component
public class NewsDataFactory {

    /** 存放数据源 */
    private final Map<String, NewsDataSource> dataSourceMap = new HashMap<>();

    /**
     * 保存所有NewsDataSource的实现类实例
     *
     * @param newsDataSources
     */
    public NewsDataFactory(List<NewsDataSource> newsDataSources) {
        newsDataSources.forEach(ds -> dataSourceMap.put(ds.getDataSourceType().getType(), ds));
    }

    /**
     * 获取所有数据源实例
     *
     * @return {@link List }<{@link NewsDataSource }>
     */
    public List<NewsDataSource> getAllDataSources() {
        return dataSourceMap.values().stream().toList();
    }

    /**
     * 获取指定数据源实例
     *
     * @param newsDataSourceTypeEnum
     * @return {@link NewsDataSource }
     */
    public NewsDataSource getDataSource(NewsDataSourceTypeEnum newsDataSourceTypeEnum) {
        return dataSourceMap.get(newsDataSourceTypeEnum.getType());
    }
}

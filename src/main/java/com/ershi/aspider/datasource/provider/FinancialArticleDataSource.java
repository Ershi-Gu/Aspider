package com.ershi.aspider.datasource.provider;

import com.ershi.aspider.datasource.domain.FinancialArticle;
import com.ershi.aspider.datasource.domain.FinancialArticleDSTypeEnum;

import java.util.List;

/**
 * 经济文章数据源接口，可实现该接口拓展数据源。
 *
 * @author Ershi-Gu.
 * @since 2025-11-10
 */
public interface FinancialArticleDataSource {

    /**
     * 数据源类型
     *
     * @return {@link FinancialArticleDSTypeEnum }
     */
    FinancialArticleDSTypeEnum getDataSourceType();

    /**
     * 获取新闻数据
     *
     * @return {@link List }<{@link FinancialArticle }>
     */
    List<FinancialArticle> getFinancialArticle();
}

package com.ershi.aspider.analysis.retriever.domain.enums;

/**
 * 检索来源枚举
 * <p>
 * 标识文章是通过何种检索方式获取的。
 *
 * @author Ershi-Gu
 */
public enum RetrievalSource {

    /** 混合检索（语义+关键词） */
    HYBRID_SEARCH,

    /** 纯向量语义检索 */
    VECTOR_SEARCH,

    /** 新闻类型过滤 */
    TYPE_FILTER,

    /** 重要性过滤 */
    IMPORTANCE_FILTER
}

package com.ershi.aspider.analysis.retriever;

/**
 * 数据检索器通用接口
 *
 * @param <Q> 查询参数类型
 * @param <R> 检索结果类型
 * @author Ershi-Gu
 */
public interface DataRetriever<Q, R> {

    /**
     * 执行检索
     *
     * @param query 查询参数
     * @return 检索结果
     */
    R retrieve(Q query);
}

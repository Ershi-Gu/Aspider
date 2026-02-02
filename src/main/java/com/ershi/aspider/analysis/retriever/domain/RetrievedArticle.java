package com.ershi.aspider.analysis.retriever.domain;

import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import lombok.Data;

/**
 * 检索到的文章（带评分）
 * <p>
 * 封装原始文章及其相关性评分和检索来源信息。
 *
 * @author Ershi-Gu
 */
@Data
public class RetrievedArticle {

    /** 原始文章 */
    private FinancialArticle article;

    /** 归一化后的相关性分数（0-1） */
    private double relevanceScore;

    /** 检索来源 */
    private RetrievalSource source;
}

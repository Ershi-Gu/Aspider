package com.ershi.aspider.analysis.retriever.domain;

import lombok.Data;

import java.util.List;

/**
 * 新闻检索结果
 *
 * @author Ershi-Gu
 */
@Data
public class NewsRetrievalResult {

    /** 检索到的文章列表（已按相关性排序） */
    private List<RetrievedArticle> articles;

    /** 检索到的总候选数量（去重前） */
    private int totalCandidates;

    /** 过滤后返回的数量 */
    private int filteredCount;
}

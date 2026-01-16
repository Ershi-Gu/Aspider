package com.ershi.aspider.data.processor.scorer.domain;

import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章评分结果
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleScoreResult {

    /**
     * 重要性评分 1-5
     * <ul>
     *   <li>5 - 重大：国家级政策、央行/证监会公告</li>
     *   <li>4 - 重要：部委政策、行业重大事件</li>
     *   <li>3 - 关注：地方政策、龙头企业动态</li>
     *   <li>2 - 一般：行业新闻、市场评论</li>
     *   <li>1 - 普通：资讯、快讯</li>
     * </ul>
     */
    private Integer importance;

    /**
     * 新闻类型
     */
    private NewsTypeEnum newsType;

    /**
     * 创建默认评分结果（普通资讯）
     */
    public static ArticleScoreResult defaultResult() {
        return new ArticleScoreResult(1, NewsTypeEnum.GENERAL);
    }
}
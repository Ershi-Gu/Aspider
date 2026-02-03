package com.ershi.aspider.analysis.agent.domain;

import com.ershi.aspider.data.datasource.domain.NewsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 政策新闻条目
 *
 * @author Ershi-Gu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyNewsItem {

    /** 新闻标题 */
    private String title;

    /** 文章详情URL */
    private String contentUrl;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 重要性评分 1-5 */
    private int importance;

    /** 新闻类型 */
    private NewsTypeEnum newsType;
}

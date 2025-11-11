package com.ershi.aspider.datasource.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政策/财经信息源数据
 *
 * @author Ershi-Gu.
 * @since 2025-11-10
 */
@Data
public class NewsDataItem {

    /** 标题 */
    private String title;

    /** 文章详情url */
    private String contentUrl;

    /** 摘要 */
    private String summary;

    /** 文章详情 */
    private String content;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 获取时间 */
    private LocalDateTime crawlTime = LocalDateTime.now();
}

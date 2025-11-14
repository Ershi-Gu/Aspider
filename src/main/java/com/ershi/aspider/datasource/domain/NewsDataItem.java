package com.ershi.aspider.datasource.domain;

import lombok.Data;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

/**
 * 政策/财经信息源数据
 *
 * @author Ershi-Gu.
 * @since 2025-11-10
 */
@Data
public class NewsDataItem {

    /** 去重标识（title + url 的 MD5） */
    private String uniqueId;

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

    /**
     * 生成唯一ID（基于 title + contentUrl）
     */
    public void generateUniqueId() {
        if (title != null && contentUrl != null) {
            String source = title + contentUrl;
            this.uniqueId = DigestUtils.md5DigestAsHex(source.getBytes());
        }
    }

}

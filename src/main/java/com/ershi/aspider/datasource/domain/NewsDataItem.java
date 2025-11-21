package com.ershi.aspider.datasource.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 标题向量（用于语义搜索） */
    private List<Double> titleVector;

    /** 文章详情url */
    private String contentUrl;

    /** 摘要 */
    private String summary;

    /** 文章详情 */
    private String content;

    /** 内容向量（用于语义搜索） */
    private List<Double> contentVector;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /** 获取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

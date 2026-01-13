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
public class FinancialArticle {

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

    /** 摘要向量（用于语义搜索） */
    private List<Double> summaryVector;

    /** 文章详情 */
    private String content;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /** 获取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime crawlTime = LocalDateTime.now();

    /**
     * 重要性评分 1-5（用于冷数据筛选）
     * <ul>
     *   <li>5 - 重大：国家级政策、央行/证监会公告</li>
     *   <li>4 - 重要：部委政策、行业重大事件</li>
     *   <li>3 - 关注：地方政策、龙头企业动态</li>
     *   <li>2 - 一般：行业新闻、市场评论</li>
     *   <li>1 - 普通：资讯、快讯</li>
     * </ul>
     */
    private Integer importance = 1;

    /** 新闻类型：POLICY/EVENT/INDUSTRY/GENERAL */
    private NewsTypeEnum newsType = NewsTypeEnum.GENERAL;

    /** 是否已完成向量化处理（采集即向量化架构下，采集完成即为true） */
    private Boolean processed = false;

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

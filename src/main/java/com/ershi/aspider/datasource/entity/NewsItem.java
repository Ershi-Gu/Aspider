package com.ershi.aspider.datasource.entity;

import java.time.LocalDateTime;

/**
 * 数据新闻实体类
 *
 * @author ershi
 */
public class NewsItem {

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 新闻详情URL
     */
    private String url;

    /**
     * 发布时间
     */
    private String publishTime;

    /**
     * 新闻摘要/简介
     */
    private String summary;

    /**
     * 爬取时间
     */
    private LocalDateTime crawlTime;

    public NewsItem() {
        this.crawlTime = LocalDateTime.now();
    }

    public NewsItem(String title, String url) {
        this.title = title;
        this.url = url;
        this.crawlTime = LocalDateTime.now();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCrawlTime() {
        return crawlTime;
    }

    public void setCrawlTime(LocalDateTime crawlTime) {
        this.crawlTime = crawlTime;
    }

    @Override
    public String toString() {
        return "NewsItem{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", publishTime='" + publishTime + '\'' +
                ", summary='" + summary + '\'' +
                ", crawlTime=" + crawlTime +
                '}';
    }
}
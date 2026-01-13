package com.ershi.aspider.datasource.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 新闻类型枚举
 * <p>
 * 用于对财经新闻进行分类，便于后续分层清理和重要性判定：
 * <ul>
 *   <li><b>POLICY</b>：政策类新闻，如国务院、央行、证监会等发布的政策公告</li>
 *   <li><b>EVENT</b>：事件类新闻，如重大行业事件、企业并购、IPO等</li>
 *   <li><b>INDUSTRY</b>：行业类新闻，如行业动态、龙头企业动态、市场分析等</li>
 *   <li><b>GENERAL</b>：一般类新闻，如资讯快讯、市场评论等</li>
 * </ul>
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
@Getter
@AllArgsConstructor
public enum NewsTypeEnum {

    /**
     * 政策类：国家/部委/地方政策公告
     */
    POLICY("POLICY", "政策类"),

    /**
     * 事件类：重大行业事件、企业并购、IPO等
     */
    EVENT("EVENT", "事件类"),

    /**
     * 行业类：行业动态、龙头企业新闻
     */
    INDUSTRY("INDUSTRY", "行业类"),

    /**
     * 一般类：资讯快讯、市场评论
     */
    GENERAL("GENERAL", "一般类");

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 根据编码获取枚举
     */
    public static NewsTypeEnum fromCode(String code) {
        for (NewsTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return GENERAL;
    }
}
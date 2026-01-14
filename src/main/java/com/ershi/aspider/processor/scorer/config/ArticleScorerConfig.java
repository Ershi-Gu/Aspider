package com.ershi.aspider.processor.scorer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文章评分配置
 *
 * @author Ershi-Gu.
 * @since 2025-01-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "processor.scorer")
public class ArticleScorerConfig {

    /**
     * 评分策略：rule（规则）/ llm（大模型）
     */
    private String strategy = "rule";

    /**
     * 规则评分配置
     */
    private RuleConfig rule = new RuleConfig();

    /**
     * 规则评分配置
     */
    @Data
    public static class RuleConfig {

        /**
         * 重大级别关键词（importance=5）
         * 国家级政策、央行/证监会公告
         */
        private List<String> criticalKeywords = List.of(
            "央行", "中国人民银行", "证监会", "银保监会", "金融监管总局",
            "国务院", "中央经济工作会议", "政治局会议",
            "降准", "降息", "加息", "LPR", "MLF",
            "IPO暂停", "熔断", "救市"
        );

        /**
         * 重要级别关键词（importance=4）
         * 部委政策、行业重大事件
         */
        private List<String> importantKeywords = List.of(
            "发改委", "工信部", "财政部", "商务部", "科技部",
            "国资委", "住建部", "交通部", "农业部",
            "十四五", "十五五", "规划", "意见", "通知", "办法",
            "并购", "重组", "借壳", "退市", "ST", "*ST",
            "违约", "暴雷", "破产", "清算"
        );

        /**
         * 关注级别关键词（importance=3）
         * 地方政策、龙头企业动态
         */
        private List<String> attentionKeywords = List.of(
            "省政府", "市政府", "地方政府",
            "茅台", "宁德时代", "比亚迪", "腾讯", "阿里", "华为", "中芯国际",
            "龙头", "头部企业", "行业领军",
            "首发", "首批", "试点", "示范"
        );

        /**
         * 政策类关键词（newsType=POLICY）
         */
        private List<String> policyKeywords = List.of(
            "政策", "法规", "条例", "办法", "规定", "通知", "意见",
            "监管", "审批", "许可", "牌照",
            "央行", "证监会", "银保监会", "发改委", "工信部", "财政部",
            "国务院", "人大", "政协"
        );

        /**
         * 事件类关键词（newsType=EVENT）
         */
        private List<String> eventKeywords = List.of(
            "并购", "收购", "重组", "IPO", "上市", "退市",
            "破产", "违约", "暴雷", "爆雷", "清算",
            "签约", "中标", "订单", "合同",
            "发布会", "峰会", "论坛"
        );

        /**
         * 行业类关键词（newsType=INDUSTRY）
         */
        private List<String> industryKeywords = List.of(
            "行业", "板块", "赛道", "产业链", "供应链",
            "半导体", "芯片", "新能源", "光伏", "储能", "锂电",
            "人工智能", "AI", "大模型", "机器人",
            "医药", "医疗", "创新药", "生物科技"
        );
    }
}
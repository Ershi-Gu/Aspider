package com.ershi.aspider.data.processor.summary;

import com.ershi.aspider.common.utils.TextTruncateUtil;
import com.ershi.aspider.data.datasource.domain.FinancialArticle;
import com.ershi.aspider.data.datasource.domain.SummarySourceEnum;
import com.ershi.aspider.data.processor.summary.config.SummaryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 摘要提取策略
 * <p>
 * 基于文章内容长度决定提取方式：
 * <ul>
 *   <li>短文本（<500字）：返回全文</li>
 *   <li>中文本（500-2000字）：智能截取</li>
 *   <li>长文本（>2000字）：返回 null，由上层决定是否调用 LLM</li>
 * </ul>
 *
 * @author Ershi-Gu
 */
@Component
public class SummaryExtractionStrategy {

    private static final Logger log = LoggerFactory.getLogger(SummaryExtractionStrategy.class);

    private final SummaryConfig config;

    public SummaryExtractionStrategy(SummaryConfig config) {
        this.config = config;
    }

    /**
     * 从文章内容提取摘要
     *
     * @param article 文章
     * @return 提取的摘要，长文本返回 null 表示需要 LLM 处理
     */
    public ExtractionResult extract(FinancialArticle article) {
        String content = article.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.debug("文章内容为空：{}", article.getTitle());
            return ExtractionResult.empty();
        }

        int length = content.length();
        SummaryConfig.Extraction extractionConfig = config.getExtraction();

        if (length <= extractionConfig.getShortTextThreshold()) {
            log.debug("短文本，直接返回全文：{} 字", length);
            return ExtractionResult.of(content, SummarySourceEnum.EXTRACTED);
        }

        if (length <= extractionConfig.getMediumTextThreshold()) {
            String truncated = TextTruncateUtil.smartTruncate(content, extractionConfig.getTruncateLength());
            log.debug("中文本，截取 {} 字，原长度 {}", extractionConfig.getTruncateLength(), length);
            return ExtractionResult.of(truncated, SummarySourceEnum.TRUNCATED);
        }

        log.debug("长文本（{} 字），需要 LLM 处理", length);
        return ExtractionResult.needLlm();
    }

    /**
     * 提取结果
     */
    public static class ExtractionResult {
        private final String summary;
        private final SummarySourceEnum source;
        private final boolean needLlm;

        private ExtractionResult(String summary, SummarySourceEnum source, boolean needLlm) {
            this.summary = summary;
            this.source = source;
            this.needLlm = needLlm;
        }

        public static ExtractionResult of(String summary, SummarySourceEnum source) {
            return new ExtractionResult(summary, source, false);
        }

        public static ExtractionResult needLlm() {
            return new ExtractionResult(null, null, true);
        }

        public static ExtractionResult empty() {
            return new ExtractionResult(null, null, false);
        }

        public String getSummary() {
            return summary;
        }

        public SummarySourceEnum getSource() {
            return source;
        }

        public boolean isNeedLlm() {
            return needLlm;
        }

        public boolean hasSummary() {
            return summary != null && !summary.trim().isEmpty();
        }
    }
}

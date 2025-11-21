package com.ershi.aspider.common.utils;

/**
 * 文本截取工具
 *
 * @author Ershi-Gu.
 * @since 2025-11-21
 */
public class TextTruncateUtil {

    /**
     * 智能截取：优先在句号处截断
     */
    public static String smartTruncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength);

        // 在句号处截断（保留至少60%的内容）
        int lastPeriod = truncated.lastIndexOf('。');
        if (lastPeriod > maxLength * 0.6) {
            return truncated.substring(0, lastPeriod + 1);
        }

        return truncated;
    }
}

package com.ershi.aspider.analysis.agent.llm;

import com.ershi.aspider.analysis.agent.domain.SignalType;
import com.ershi.aspider.analysis.agent.domain.TrendView;
import com.ershi.aspider.analysis.agent.llm.dto.PolicyLlmResponse;
import com.ershi.aspider.analysis.agent.llm.dto.SectorLlmResponse;
import com.ershi.aspider.analysis.agent.llm.dto.TrendLlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM响应验证器
 *
 * 验证LLM返回的响应对象，确保必填字段存在且枚举值有效
 * 验证失败时自动修复为默认值，并记录警告日志
 *
 * @author Ershi-Gu
 */
@Component
@Slf4j
public class LlmResponseValidator {

    /** 列表字段最大长度限制 */
    private static final int MAX_LIST_SIZE = 5;

    /** 单条文本最大长度限制 */
    private static final int MAX_TEXT_LENGTH = 500;

    /**
     * 验证并修复PolicyLlmResponse
     *
     * @param response LLM响应
     * @return 验证并修复后的响应
     */
    public PolicyLlmResponse validateAndFix(PolicyLlmResponse response) {
        if (response == null) {
            log.warn("PolicyLlmResponse为空，返回默认值");
            return PolicyLlmResponse.builder()
                .signal(SignalType.NEUTRAL)
                .coreDrivers(new ArrayList<>())
                .potentialRisks(new ArrayList<>())
                .build();
        }

        // 验证signal
        if (response.getSignal() == null) {
            log.warn("PolicyLlmResponse.signal为空，设置为NEUTRAL");
            response.setSignal(SignalType.NEUTRAL);
        }

        // 验证并截断coreDrivers
        response.setCoreDrivers(sanitizeList(response.getCoreDrivers(), "coreDrivers"));

        // 验证并截断potentialRisks
        response.setPotentialRisks(sanitizeList(response.getPotentialRisks(), "potentialRisks"));

        return response;
    }

    /**
     * 验证并修复SectorLlmResponse
     *
     * @param response LLM响应
     * @return 验证并修复后的响应
     */
    public SectorLlmResponse validateAndFix(SectorLlmResponse response) {
        if (response == null) {
            log.warn("SectorLlmResponse为空，返回默认值");
            return SectorLlmResponse.builder()
                .capitalSignal(SignalType.NEUTRAL)
                .sentimentSignal(SignalType.NEUTRAL)
                .build();
        }

        // 验证capitalSignal
        if (response.getCapitalSignal() == null) {
            log.warn("SectorLlmResponse.capitalSignal为空，设置为NEUTRAL");
            response.setCapitalSignal(SignalType.NEUTRAL);
        }

        // 验证sentimentSignal
        if (response.getSentimentSignal() == null) {
            log.warn("SectorLlmResponse.sentimentSignal为空，设置为NEUTRAL");
            response.setSentimentSignal(SignalType.NEUTRAL);
        }

        return response;
    }

    /**
     * 验证并修复TrendLlmResponse
     *
     * @param response LLM响应
     * @return 验证并修复后的响应
     */
    public TrendLlmResponse validateAndFix(TrendLlmResponse response) {
        if (response == null) {
            log.warn("TrendLlmResponse为空，返回默认值");
            return TrendLlmResponse.builder()
                .signal(SignalType.NEUTRAL)
                .shortTerm(TrendView.builder().viewpoint("数据不足").basis("").build())
                .midTerm(TrendView.builder().viewpoint("数据不足").basis("").build())
                .riskWarnings(new ArrayList<>())
                .build();
        }

        // 验证signal
        if (response.getSignal() == null) {
            log.warn("TrendLlmResponse.signal为空，设置为NEUTRAL");
            response.setSignal(SignalType.NEUTRAL);
        }

        // 验证shortTerm
        if (response.getShortTerm() == null) {
            log.warn("TrendLlmResponse.shortTerm为空，设置默认值");
            response.setShortTerm(TrendView.builder().viewpoint("数据不足").basis("").build());
        } else {
            sanitizeTrendView(response.getShortTerm(), "shortTerm");
        }

        // 验证midTerm
        if (response.getMidTerm() == null) {
            log.warn("TrendLlmResponse.midTerm为空，设置默认值");
            response.setMidTerm(TrendView.builder().viewpoint("数据不足").basis("").build());
        } else {
            sanitizeTrendView(response.getMidTerm(), "midTerm");
        }

        // 验证并截断riskWarnings
        response.setRiskWarnings(sanitizeList(response.getRiskWarnings(), "riskWarnings"));

        return response;
    }

    /**
     * 清理并截断列表字段
     *
     * @param list      原始列表
     * @param fieldName 字段名（用于日志）
     * @return 清理后的列表
     */
    private List<String> sanitizeList(List<String> list, String fieldName) {
        if (list == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(list.size(), MAX_LIST_SIZE); i++) {
            String item = list.get(i);
            if (item != null && !item.isBlank()) {
                // 截断过长文本
                if (item.length() > MAX_TEXT_LENGTH) {
                    log.warn("{}[{}]长度超限，已截断: {} -> {}", fieldName, i, item.length(), MAX_TEXT_LENGTH);
                    item = item.substring(0, MAX_TEXT_LENGTH) + "...";
                }
                result.add(sanitizeText(item));
            }
        }

        if (list.size() > MAX_LIST_SIZE) {
            log.warn("{}列表超过最大长度{}，已截断", fieldName, MAX_LIST_SIZE);
        }

        return result;
    }

    /**
     * 清理TrendView字段
     */
    private void sanitizeTrendView(TrendView view, String fieldName) {
        if (view.getViewpoint() != null && view.getViewpoint().length() > MAX_TEXT_LENGTH) {
            log.warn("{}.viewpoint长度超限，已截断", fieldName);
            view.setViewpoint(view.getViewpoint().substring(0, MAX_TEXT_LENGTH) + "...");
        }
        if (view.getBasis() != null && view.getBasis().length() > MAX_TEXT_LENGTH) {
            log.warn("{}.basis长度超限，已截断", fieldName);
            view.setBasis(view.getBasis().substring(0, MAX_TEXT_LENGTH) + "...");
        }

        // 清理文本
        if (view.getViewpoint() != null) {
            view.setViewpoint(sanitizeText(view.getViewpoint()));
        }
        if (view.getBasis() != null) {
            view.setBasis(sanitizeText(view.getBasis()));
        }
    }

    /**
     * 清理文本内容（移除潜在注入字符）
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        // 移除控制字符和潜在的模板注入字符
        return text
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "") // 控制字符
            .replaceAll("\\{\\{", "{") // 防止模板注入
            .replaceAll("\\}\\}", "}");
    }
}

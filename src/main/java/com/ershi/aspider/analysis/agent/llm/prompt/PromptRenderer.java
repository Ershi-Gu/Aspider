package com.ershi.aspider.analysis.agent.llm.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt渲染器
 *
 * 使用 {{变量名}} 语法进行占位符替换
 *
 * @author Ershi-Gu
 */
@Component
@Slf4j
public class PromptRenderer {

    /** 占位符正则：匹配 {{变量名}} */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * 渲染Prompt模板
     *
     * @param template  模板文本
     * @param variables 变量映射
     * @return 渲染后的Prompt
     * @throws PromptRenderException 变量缺失时抛出
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null || template.isBlank()) {
            throw new PromptRenderException("模板内容不能为空");
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);

            if (value == null) {
                throw new PromptRenderException("Prompt渲染失败：变量缺失 {{" + variableName + "}}");
            }

            // 转义特殊字符防止正则替换问题
            String replacement = Matcher.quoteReplacement(value.toString());
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        String rendered = result.toString();
        log.debug("Prompt渲染完成，模板长度={}，渲染后长度={}", template.length(), rendered.length());
        return rendered;
    }
}

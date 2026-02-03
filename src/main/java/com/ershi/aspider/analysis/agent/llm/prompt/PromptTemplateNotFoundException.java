package com.ershi.aspider.analysis.agent.llm.prompt;

/**
 * Prompt模板未找到异常
 *
 * @author Ershi-Gu
 */
public class PromptTemplateNotFoundException extends RuntimeException {

    public PromptTemplateNotFoundException(String message) {
        super(message);
    }

    public PromptTemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

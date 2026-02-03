package com.ershi.aspider.analysis.agent.llm.prompt;

/**
 * Prompt渲染异常
 *
 * @author Ershi-Gu
 */
public class PromptRenderException extends RuntimeException {

    public PromptRenderException(String message) {
        super(message);
    }

    public PromptRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}

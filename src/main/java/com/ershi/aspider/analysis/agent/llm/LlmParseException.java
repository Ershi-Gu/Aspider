package com.ershi.aspider.analysis.agent.llm;

/**
 * LLM响应解析异常
 *
 * 当LLM返回的JSON无法解析时抛出
 * 区别于LlmExecutionException（LLM调用失败），用于触发不同的降级消息
 *
 * @author Ershi-Gu
 */
public class LlmParseException extends RuntimeException {

    /** 原始响应内容 */
    private final String rawResponse;

    public LlmParseException(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse;
    }

    public LlmParseException(String message, String rawResponse, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    /**
     * 获取原始响应内容（用于调试日志）
     */
    public String getRawResponse() {
        return rawResponse;
    }
}

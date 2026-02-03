package com.ershi.aspider.analysis.agent.llm;

/**
 * LLM执行异常
 *
 * 表示LLM调用、响应解析或校验失败
 * Agent捕获此异常后应触发规则降级
 *
 * @author Ershi-Gu
 */
public class LlmExecutionException extends RuntimeException {

    public LlmExecutionException(String message) {
        super(message);
    }

    public LlmExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

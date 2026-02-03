package com.ershi.aspider.analysis.agent.core;

import com.ershi.aspider.analysis.agent.domain.AgentType;

/**
 * Agent 统一接口
 *
 * @param <I> 输入类型
 * @param <O> 输出类型
 * @author Ershi-Gu
 */
public interface Agent<I, O> {

    /**
     * 执行分析
     *
     * @param input 输入数据
     * @return 分析结果
     */
    O analyze(I input);

    /**
     * 获取Agent类型
     *
     * @return Agent类型枚举
     */
    AgentType getAgentType();

    /**
     * 获取Agent名称（用于日志）
     *
     * @return Agent名称
     */
    default String getName() {
        return getAgentType().name();
    }
}

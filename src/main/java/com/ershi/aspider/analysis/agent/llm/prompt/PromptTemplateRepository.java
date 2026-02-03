package com.ershi.aspider.analysis.agent.llm.prompt;

import com.ershi.aspider.analysis.agent.domain.AgentType;

/**
 * Prompt模板仓库接口
 *
 * 负责按Agent类型加载对应的Prompt模板文本
 *
 * @author Ershi-Gu
 */
public interface PromptTemplateRepository {

    /**
     * 获取指定Agent的Prompt模板
     *
     * @param agentType Agent类型
     * @return Prompt模板文本
     * @throws PromptTemplateNotFoundException 模板不存在时抛出
     */
    String getTemplate(AgentType agentType);

    /**
     * 检查指定Agent的模板是否存在
     *
     * @param agentType Agent类型
     * @return 是否存在
     */
    boolean hasTemplate(AgentType agentType);
}
package com.ershi.aspider.analysis.agent.llm.prompt;

import com.ershi.aspider.analysis.agent.domain.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Classpath的Prompt模板仓库实现
 *
 * 从 resources/prompts/ 目录加载模板，启动时缓存以提高性能
 *
 * @author Ershi-Gu
 */
@Component
@Slf4j
public class ClasspathPromptTemplateRepository implements PromptTemplateRepository {

    /** 模板目录前缀 */
    private static final String TEMPLATE_PATH_PREFIX = "prompts/";

    /** 模板文件后缀 */
    private static final String TEMPLATE_FILE_SUFFIX = "-agent.prompt";

    /** 模板缓存 */
    private final Map<AgentType, String> templateCache = new ConcurrentHashMap<>();

    @Override
    public String getTemplate(AgentType agentType) {
        return templateCache.computeIfAbsent(agentType, this::loadTemplate);
    }

    @Override
    public boolean hasTemplate(AgentType agentType) {
        if (templateCache.containsKey(agentType)) {
            return true;
        }
        String path = buildTemplatePath(agentType);
        ClassPathResource resource = new ClassPathResource(path);
        return resource.exists();
    }

    /**
     * 加载模板文件
     */
    private String loadTemplate(AgentType agentType) {
        String path = buildTemplatePath(agentType);
        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            throw new PromptTemplateNotFoundException(
                "Prompt模板不存在: " + path + "，AgentType=" + agentType);
        }

        try (InputStream is = resource.getInputStream()) {
            String template = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            log.info("加载Prompt模板成功: {}，长度={}", path, template.length());
            return template;
        } catch (IOException e) {
            throw new PromptTemplateNotFoundException(
                "读取Prompt模板失败: " + path, e);
        }
    }

    /**
     * 构建模板文件路径
     * 例：POLICY -> prompts/policy-agent.prompt
     */
    private String buildTemplatePath(AgentType agentType) {
        return TEMPLATE_PATH_PREFIX + agentType.name().toLowerCase() + TEMPLATE_FILE_SUFFIX;
    }
}

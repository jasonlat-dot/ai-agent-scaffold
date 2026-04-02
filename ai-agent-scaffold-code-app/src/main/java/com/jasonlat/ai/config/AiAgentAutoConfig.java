package com.jasonlat.ai.config;

import com.alibaba.fastjson2.JSON;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import com.jasonlat.ai.domain.agent.service.IAmoryService;
import com.jasonlat.ai.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author jasonlat
 * 2026-03-31  19:35
 */
@Data
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(AiAgentAutoConfig.class);

    /** 智能体名称分隔符 */
    private static final String NAME_SEPARATOR = "#";

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;
    @Resource
    private IAmoryService amoryService;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        try {
            Map<String, AiAgentConfigTableVO> aiAgentConfigTables = aiAgentAutoConfigProperties.getTables();
            // 处理 appName 和 agentId, 确保不能重复, 也不能为空
            processAppName(aiAgentConfigTables);

            // 获取 tables 并处理名称拼接
            List<AiAgentConfigTableVO> tables = new ArrayList<>(aiAgentAutoConfigProperties.getTables().values());
            processAgentName(tables);
            log.info("Initializing Ai agent yml config success!  -> \n 【{}】", JSON.toJSONString(aiAgentConfigTables));

            // 接收智能体 - 执行装配智能体
            log.info("Initialize AI agent node start...");
            amoryService.acceptArmoryAgents(new ArrayList<>(aiAgentAutoConfigProperties.getTables().values()));
            log.info("Initialize AI agent node end");
        } catch (Exception e) {
            log.error("Error occurred while initializing AI agent", e);
            throw new RuntimeException(e);
        }
    }

    private static void processAppName(Map<String, AiAgentConfigTableVO> aiAgentConfigTables) {
        Collection<AiAgentConfigTableVO> aiAgentConfigTableVOS = aiAgentConfigTables.values();
        List<String> appNames = new ArrayList<>(aiAgentConfigTableVOS.size());
        List<String> agentIds = new ArrayList<>(aiAgentConfigTableVOS.size());
        for (AiAgentConfigTableVO aiAgentConfigTableVO : aiAgentConfigTableVOS) {
            String appName = aiAgentConfigTableVO.getAppName();
            String agentId = aiAgentConfigTableVO.getAgentDefinition().getAgentId();
            if (appNames.contains(appName) || agentIds.contains(agentId)) {
                throw new AppException("appName or appId is repeated");
            }
            if (StringUtils.isBlank(appName) || StringUtils.isBlank(agentId)) {
                throw new AppException("appName or appName is empty");
            }
            appNames.add(appName);
            agentIds.add(agentId);
        }
    }

    private void processAgentName(List<AiAgentConfigTableVO> table) {
        table.forEach(aiAgentConfigTableVO -> {
            // 获取 agents
            List<AiAgentConfigTableVO.Module.Agent> agents = aiAgentConfigTableVO.getModule().getLlmAgents();
            if (agents != null && !agents.isEmpty()) {
                for (AiAgentConfigTableVO.Module.Agent agent : agents) {
                    // 拼接格式: appName#name
                    agent.setName(aiAgentConfigTableVO.getAppName() + NAME_SEPARATOR + agent.getName());
                }
            }
            // 处理workflow-agents
            List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();
            if (agentWorkflows != null && !agentWorkflows.isEmpty()) {
                for (AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow : agentWorkflows) {
                    // 拼接格式: appName#name
                    agentWorkflow.setName(aiAgentConfigTableVO.getAppName() + NAME_SEPARATOR + agentWorkflow.getName());
                    // 处理 agent-workflows 的 subAgents
                    List<String> prefixedSubAgents = getPrefixedSubAgents(aiAgentConfigTableVO.getAppName(), agentWorkflow);
                    agentWorkflow.setSubAgents(prefixedSubAgents);
                }
            }
            // 处理 runner
            AiAgentConfigTableVO.Module.Runner runner = aiAgentConfigTableVO.getModule().getRunner();
            if (runner == null || StringUtils.isBlank(runner.getAgentName())) {
                throw new AppException("runner.agentName cannot be empty");
            }
            // 拼接格式: appName#name
            runner.setAgentName(aiAgentConfigTableVO.getAppName() + NAME_SEPARATOR + runner.getAgentName());
        });

    }

    private static @NotNull List<String> getPrefixedSubAgents(String appName, AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow) {
        List<String> subAgents = agentWorkflow.getSubAgents();
        if (subAgents == null || subAgents.isEmpty()) {
            throw new AppException("agent-workflows.subAgents cannot be empty");
        }
        List<String> prefixedSubAgents = new ArrayList<>(subAgents.size());
        subAgents.forEach(subAgent -> {
            // 拼接格式: appName#name
            subAgent = appName + NAME_SEPARATOR + subAgent;
            prefixedSubAgents.add(subAgent);
        });
        return prefixedSubAgents;
    }
}

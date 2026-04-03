package com.jasonlat.ai.domain.agent.service.amory.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonlat.ai.domain.agent.model.entity.ArmoryCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.jasonlat.ai.domain.agent.service.amory.AbstractAmorySupport;
import com.jasonlat.ai.domain.agent.service.amory.factory.DefaultArmoryFactory;
import com.jasonlat.ai.domain.agent.service.amory.mcp.client.ToolMcpCreateService;
import com.jasonlat.ai.domain.agent.service.amory.mcp.client.factory.DefaultMcpClientFactory;
import com.jasonlat.design.framework.tree.StrategyHandler;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jasonlat
 * 2026-04-01  18:45
 */
@Service
public class ChatModelNode extends AbstractAmorySupport {

    private static final Logger log = LoggerFactory.getLogger(ChatModelNode.class);

    @Resource
    private AgentNode agentNode;

    @Resource
    private DefaultMcpClientFactory mcpClientFactory;

    /**
     * 业务流程处理方法
     * <p>
     * 子类需要实现此方法来定义具体的业务处理逻辑。
     * 该方法在异步数据加载完成后执行。
     * </p>
     *
     * @param requestParameter 请求参数
     * @param dynamicContext   动态上下文
     * @return 处理结果
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - ChatModelNode");
        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        // 注册默认配置的 chatModel
        registerDefaultChatModelConfig(aiAgentConfigTableVO, dynamicContext);
        // 处理个性化的 chatModel
        registerLlmAgentChatModelConfig(aiAgentConfigTableVO, dynamicContext);
        // 路由下一个节点
        return router(requestParameter, dynamicContext);
    }

    private void registerDefaultChatModelConfig(AiAgentConfigTableVO aiAgentConfigTableVO, DefaultArmoryFactory.DynamicContext dynamicContext) {
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        // 获取默认配置的 openAiApi
        OpenAiApi openAiApi = dynamicContext.getOpenAiApiMap().get(getDefaultAiApiMapKey(aiAgentConfigTableVO.getAppName()));
        // 构建默认的 mcpSyncClient
        List<ToolCallback> toolCallbacks = getMcpSyncClients(chatModelConfig);
        // 构建默认的 chatModel
        ChatModel chatModel = getChatModel(openAiApi, chatModelConfig, toolCallbacks);
        dynamicContext.getChatModelMap().put(getDefaultChatModelMapKey(aiAgentConfigTableVO.getAppName()), chatModel);
    }

    private void registerLlmAgentChatModelConfig(AiAgentConfigTableVO aiAgentConfigTableVO, DefaultArmoryFactory.DynamicContext dynamicContext) {
        AiAgentConfigTableVO.Module.ChatModel defaultChatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        List<AiAgentConfigTableVO.Module.Agent> llmAgents = aiAgentConfigTableVO.getModule().getLlmAgents();
        if (llmAgents == null || llmAgents.isEmpty()) {
            throw new RuntimeException("module.llmAgents is empty");
        }
        llmAgents.forEach(llmAgent -> {
            AiAgentConfigTableVO.Module.ChatModel llmAgentChatModelConfig = llmAgent.getChatModel();
            if (null == llmAgentChatModelConfig) {
                // 如果没有自定义配置的 chatModel， 则使用默认的 chatModel
                llmAgentChatModelConfig = defaultChatModelConfig;
            }
            // 获取自定义配置的 openAiApi
            OpenAiApi llmAgentOpenAiApi = dynamicContext.getOpenAiApiMap().get(llmAgent.getName());
            if (null == llmAgentOpenAiApi ) {
                // 如果没有自定义配置的 openAi， 则使用默认的 openAiApi
                llmAgentOpenAiApi = dynamicContext.getOpenAiApiMap().get(getDefaultAiApiMapKey(aiAgentConfigTableVO.getAppName()));
            }
            // 构建自定义的 mcpSyncClient
            List<ToolCallback> llmAgentToolCallbacks = getMcpSyncClients(defaultChatModelConfig);
            // 构建自定义的 chatModel
            ChatModel llmAgentChatModel = getChatModel(llmAgentOpenAiApi, llmAgentChatModelConfig, llmAgentToolCallbacks);
            dynamicContext.getChatModelMap().put(llmAgent.getName(), llmAgentChatModel);
        });
    }


    /**
     * 获取待执行的策略处理器
     * <p>
     * 根据请求参数和动态上下文的内容，选择并返回合适的策略处理器。
     * 实现类需要根据具体的业务规则来实现策略选择逻辑。
     * </p>
     *
     * @param requestParameter 请求参数，用于确定策略选择的依据
     * @param dynamicContext   动态上下文，包含策略选择过程中需要的额外信息
     * @return 选择的策略处理器，如果没有找到合适的策略则返回null
     * @throws Exception 策略选择过程中可能抛出的异常
     */
    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return agentNode;
    }


    private static @NotNull ChatModel getChatModel(OpenAiApi openAiApi, AiAgentConfigTableVO.Module.ChatModel chatModelConfig, List<ToolCallback> toolCallbacks) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(toolCallbacks)
                        .build())
                .build();
    }

    private @NotNull List<ToolCallback> getMcpSyncClients(AiAgentConfigTableVO.Module.ChatModel chatModelConfig) {
        List<ToolCallback> toolCallbackList = new ArrayList<>(8);
        chatModelConfig.getToolMcpList().forEach(toolMcp -> {
            try {
                ToolMcpCreateService toolMcpCreateService = mcpClientFactory.getToolMcpCreateService(toolMcp);
                ToolCallback[] toolCallbacks = toolMcpCreateService.buildToolCallback(toolMcp);
                toolCallbackList.addAll(List.of(toolCallbacks));
            } catch (Exception e) {
                log.error("创建 mcpSyncClient 失败", e);
            }
        });
        return toolCallbackList;
    }

}

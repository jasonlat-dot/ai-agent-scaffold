package com.jasonlat.ai.domain.agent.service.amory.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonlat.ai.domain.agent.model.entity.ArmoryCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.jasonlat.ai.domain.agent.service.amory.AbstractAmorySupport;
import com.jasonlat.ai.domain.agent.service.amory.factory.DefaultArmoryFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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
        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();
        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();

        List<McpSyncClient> mcpSyncClients = new ArrayList<>(8);
        chatModelConfig.getToolMcpList().forEach(toolMcp -> {
            try {
                McpSyncClient mcpSyncClient = createMcpSyncClient(toolMcp);
                mcpSyncClients.add(mcpSyncClient);
            } catch (Exception e) {
                log.error("创建 mcpSyncClient 失败", e);
            }
        });

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(SyncMcpToolCallbackProvider.builder()
                                .mcpClients(mcpSyncClients).build()
                                .getToolCallbacks())
                        .build())
                .build();
        dynamicContext.setChatModel(chatModel);

        return router(requestParameter, dynamicContext);
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

    private McpSyncClient createMcpSyncClient(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        // sse or stdio
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();
        if (null != sseConfig) {
            // sse 形式的，需要拆解为 baseUri 和 sseEndpoint
            // http://appbuilder.baidu.com/v2/ai_search/mcp/sse?api_key=xxx

            String originalBaseUri = sseConfig.getBaseUri();
            String sseEndpoint = sseConfig.getSseEndpoint();
            String baseUri = originalBaseUri;
            if (StringUtils.isBlank(sseEndpoint)) {
                // 说明只配置了 baseUri （可能是完整的sse地址，比如：http://appbuilder.baidu.com/v2/ai_search/mcp/sse?api_key=xxx）
                // 也可能只有：baseUri没有sseEndpoint 如：http://appbuilder.baidu.com
                URL url = new URL(originalBaseUri);

                String protocol = url.getProtocol();
                String host = url.getHost();
                int port = url.getPort();

                String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;
                int index = originalBaseUri.indexOf(baseUrl);
                if (index != -1) {
                    sseEndpoint = originalBaseUri.substring(index + baseUrl.length());
                }
                baseUri = baseUrl;
            }
            // 兜底处理：如果 sseEndpoint 为空，则默认为 /sse
            sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

            HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                    .builder(baseUri)
                    .sseEndpoint(sseEndpoint)
                    .build();

            McpSyncClient mcpSyncClient = McpClient
                    .sync(sseClientTransport)
                    .requestTimeout(Duration.ofMillis(sseConfig.getRequestTimeout())).build();
            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

            log.info("tool sse mcp initialize {}", initialize);
            return mcpSyncClient;
        }

        if (null != stdioConfig) {
            AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters
                    = stdioConfig.getServerParameters();

            ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
                    .args(serverParameters.getArgs())
                    .env(serverParameters.getEnv())
                    .build();

            McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper())))
                    .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();

            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

            log.info("tool stdio mcp initialize {}", initialize);
            return mcpSyncClient;
        }

        throw new RuntimeException("tool mcp [sse and stdio] all is null!");
    }
}

package com.jasonlat.ai.test.tools;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;

import java.net.UnknownHostException;
import java.time.Duration;

/**
 * @author jasonlat
 * 2026-03-30  19:38
 */
@Slf4j
public class SpringAiToolsTest {
    public static void main(String[] args) throws UnknownHostException {

        ZhiPuAiApi zhiPuAiApi = ZhiPuAiApi.builder()
                .apiKey("242234771dd941379f67a52f8ac5a63a.64S1Yv17xk9KtsJG")
                .completionsPath("/v4/chat/completions")
                .embeddingsPath("/v4/embeddings")
                .build();
        ZhiPuAiChatModel zhiPuAiChatModel = new ZhiPuAiChatModel(zhiPuAiApi, ZhiPuAiChatOptions.builder()
                .model("glm-4.7-flash")
                .toolCallbacks(SyncMcpToolCallbackProvider.builder()
                        .mcpClients(sseMcpClient()).build()
                        .getToolCallbacks())
                .temperature(0.7)
                .build()
        );

        String call = zhiPuAiChatModel.call("你有哪些工具？");
        log.info(call);
    }

    /**
     * 百度搜索MCP服务(url)；https://sai.baidu.com/zh/detail/e014c6ffd555697deabf00d058baf388
     * 百度搜索MCP服务(key)；https://console.bce.baidu.com/iam/?_=1753597622044#/iam/apikey/list
     */
    public static McpSyncClient sseMcpClient() {

        // 自己申请 api_key
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder("http://appbuilder.baidu.com/v2/ai_search/mcp/")
                .sseEndpoint("sse?api_key=bce-v3/ALTAK-DAjqmKUyljHZzKsZnJ8x6/88724397eb336d831b13364919cbade256c459a1")
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(360)).build();
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP Initialized {}", init_sse);

        return mcpSyncClient;
    }
}

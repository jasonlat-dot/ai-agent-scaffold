package com.jasonlat.ai.test.tools;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;

@Slf4j
public class SpringAiSkillsTest {
    public static void main(String[] args)  {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://open.bigmodel.cn/api/paas/")
                .apiKey("242234771dd941379f67a52f8ac5a63a.64S1Yv17xk9KtsJG")
                .completionsPath("v4/chat/completions")
                .embeddingsPath("v4/embeddings")
                .build();

        ToolCallback skills = SkillsTool.builder().addSkillsResource(new ClassPathResource("skills")).build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .toolCallbacks(new ArrayList<>() {
                            { add(skills); }
                        })
                        .model("GLM-4.7-Flash")
                        .build())
                .build();

        String call = chatModel.call("你有哪些技能？");
        log.info(call);
    }
}
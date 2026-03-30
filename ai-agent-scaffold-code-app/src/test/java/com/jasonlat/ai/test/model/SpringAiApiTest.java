package com.jasonlat.ai.test.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author jasonlat
 * 2026-03-30  19:17
 */
@Slf4j
public class SpringAiApiTest {

    public static void main(String[] args) throws UnknownHostException {

        ZhiPuAiApi zhiPuAiApi = ZhiPuAiApi.builder()
                .apiKey("242234771dd941379f67a52f8ac5a63a.64S1Yv17xk9KtsJG")
                .completionsPath("/v4/chat/completions")
                .embeddingsPath("/v4/embeddings")
                .build();
        ZhiPuAiChatModel zhiPuAiChatModel = new ZhiPuAiChatModel(zhiPuAiApi, ZhiPuAiChatOptions.builder()
                .model("glm-4.7-flash")
                .toolCallbacks()
                .temperature(0.7)
                .build()
        );
        String call = zhiPuAiChatModel.call("1+1");
        log.info(call);
    }
}

package com.jasonlat.ai.test.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;

import java.net.UnknownHostException;

/**
 * @author jasonlat
 * 2026-03-30  19:29
 */
@Slf4j
public class LangChain4jApiTest {

    public static void main(String[] args) throws UnknownHostException {

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .baseUrl("xxx")
                .apiKey("xxx")
                .modelName("")
                .build();
        String call = openAiChatModel.chat("1+1");
        log.info(call);
    }

}

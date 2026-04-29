package com.jasonlat.ai.test;

import com.alibaba.fastjson2.JSON;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.models.springai.SpringAI;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.jasonlat.ai.domain.agent.service.amory.matter.patch.LocalSpringAI;
import com.jasonlat.ai.types.enums.ResponseCode;
import com.jasonlat.ai.types.exception.AppException;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.util.Base64;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    public static void main(String[] args) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceAsStream = classLoader.getResourceAsStream("dog.png");
        Resource resource = new ClassPathResource("dog.png", classLoader);
        assert resourceAsStream != null;

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://api.jasonlat.com/")
                .apiKey("sk-9015ff0e06e6798b520fe274dab46df42c854b4c5b88cf5743abe76727923c3d")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-5.4")
                        .build())
                .build();

        // 模型测试，没问题可以识别图片
//        ChatResponse response = chatModel.call(new Prompt(
//                UserMessage.builder()
//                        .text("请描述这张图片的主要内容，并说明图中物品的可能用途。")
//                        .media(Media.builder()
//                                .mimeType(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE))
//                                .data(resource)
//                                .build())
//                        .build(),
//                OpenAiChatOptions.builder()
//                        .model("gpt-5.4") // glm-4.6v-flash glm-4v-flash
//                        .build()));
//
//        System.out.println("测试结果" + JSON.toJSONString(response));


        // agent 测试
        LlmAgent agent = LlmAgent.builder()
                .name("test")
                .description("Chess coach agent")
                .model(new LocalSpringAI(chatModel))
                .instruction("""
                        You are a knowledgeable chess coach
                        who helps chess players train and sharpen their chess skills.
                        """)
                .build();

        InMemoryRunner runner = new InMemoryRunner(agent);

        Session session = runner
                .sessionService()
                .createSession("test", "jasonlat")
                .blockingGet();

        String base64 = new String(
                classLoader.getResourceAsStream("image.txt").readAllBytes()
        );
        RunConfig runConfig = RunConfig.builder()
                .setStreamingMode(RunConfig.StreamingMode.SSE) // 逐字返回
                .setMaxLlmCalls(20)
                .build();

        Flowable<Event> events = runner.runAsync("jasonlat", session.id(),
                Content.fromParts(Part.fromText("这是什么图片"),
                        Part.fromBytes(decodeBase64(base64), MimeTypeUtils.IMAGE_PNG_VALUE)), runConfig);

        System.out.print("\nAgent > ");
        events.blockingForEach(event -> System.out.println(event.stringifyContent()));

    }

    private static byte[] decodeBase64(String data) {
        String raw = data;
        int commaIndex = data.indexOf(',');
        if (data.startsWith("data:") && commaIndex >= 0) {
            raw = data.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            throw new AppException(ResponseCode.CLIENT_A0427.getCode(), "inlineData base64 decode failed", e);
        }
    }

}

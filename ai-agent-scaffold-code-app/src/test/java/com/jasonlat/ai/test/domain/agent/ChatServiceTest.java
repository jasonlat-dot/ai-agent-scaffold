package com.jasonlat.ai.test.domain.agent;

import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.domain.agent.service.IChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author jasonlat
 * 2026-04-04  11:41
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ChatServiceTest {
    @Resource
    private IChatService chatService;

    @Value("classpath:dog.png")
    private org.springframework.core.io.Resource  resource;

    @Test
    public void test_handleMessage() {
        List<String> result = chatService.handleMessage("100001", "jasonlat", "编写冒泡排序");
        log.info("测试结果:{}", result);
    }

    @Test
    public void test_handleMessage_02() {
        List<String> result = chatService.handleMessage("testAgent2", "jasonlat", "你具备哪些能力");
        log.info("测试结果:{}", result);
    }

    @Test
    public void test_handleMessage_03() throws IOException {

        String agentID = "testAgent2";
        String userID = "jasonlat";

        String sessionId = chatService.createSession(agentID, userID);

        ChatCommandEntity chatCommandEntity = ChatCommandEntity.builder()
                .agentId(agentID)
                .userId(userID)
                .sessionId(sessionId)
                .texts(List.of(new ChatCommandEntity.Content.Text("这是一张什么样的图片")))
                .files(List.of())
                .inlineData(List.of(new ChatCommandEntity.Content.InlineData(resource.getInputStream().readAllBytes(), MimeTypeUtils.IMAGE_PNG_VALUE)))
                .build();
        List<String> strings = chatService.handleMessage(chatCommandEntity);
        log.info("测试结果:{}", strings);
    }
}

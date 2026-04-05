package com.jasonlat.ai.test;

import com.alibaba.fastjson.JSON;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentAutoConfigTest {

    @Resource
    private ApplicationContext applicationContext;

    @Test
    public void test_agent() throws InterruptedException {
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "jasonlat")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("编写冒泡排序"));
        Flowable<Event> events = runner.runAsync("jasonlat", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));

        new CountDownLatch(1).await();
    }

    @Test
    public void test_handlerMessage_02(){
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100002", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "jasonlat")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("你具备哪些能力"));
        Flowable<Event> events = runner.runAsync("jasonlat", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));
    }

    @Test
    public void test_handlerMessage_03(){
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("testAgent2", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "jasonlat")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("你能调用哪些mcp工具"));
        Flowable<Event> events = runner.runAsync("jasonlat", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));
    }

    @Test
    public void test_handlerMessage_04(){
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("testAgent2", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "jasonlat")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("你有哪" +
                "些skills技能"));
        Flowable<Event> events = runner.runAsync("jasonlat", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));
    }

}

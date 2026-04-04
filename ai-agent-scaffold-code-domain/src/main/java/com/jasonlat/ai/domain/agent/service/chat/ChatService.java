package com.jasonlat.ai.domain.agent.service.chat;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.jasonlat.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import com.jasonlat.ai.domain.agent.service.IChatService;
import com.jasonlat.ai.domain.agent.service.amory.factory.DefaultArmoryFactory;
import com.jasonlat.ai.types.enums.ResponseCode;
import com.jasonlat.ai.types.exception.AppException;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jasonlat
 * 2026-04-04  10:46
 */
@Slf4j
@Service
public class ChatService implements IChatService {

    @Resource
    private DefaultArmoryFactory armoryFactory;

    @Resource
    AiAgentAutoConfigProperties agentAutoConfigProperties;

    // 用户会话， 如果是分布式，需要使用分布式缓存 比如Redis
    private final Map<String, String> userSessions = new ConcurrentHashMap<>(8);

    @Override
    public String createSession(String agentId, String userId) {
        // 获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = armoryFactory.getAiAgentRegisterVO(agentId);
        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.CLIENT_A0301.getInfo());
        }
        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        return userSessions.computeIfAbsent(userId, uid -> {
            Session session = runner.sessionService().createSession(appName, uid)
                    .blockingGet();
            return session.id();
        });
    }

    @Override
    public String createSession(ChatCommandEntity chatCommandEntity) {
        return createSession(chatCommandEntity.getAgentId(), chatCommandEntity.getUserId());
    }

    @Override
    public List<AiAgentConfigTableVO.AgentDefinition> queryAgentConfigList() {
        Map<String, AiAgentConfigTableVO> tables = agentAutoConfigProperties.getTables();

        if (tables == null || tables.values().isEmpty()) return Collections.emptyList();

        return tables.values().stream()
                .map(AiAgentConfigTableVO::getAgentDefinition)
                .toList();
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = armoryFactory.getAiAgentRegisterVO(agentId);
        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.CLIENT_A0301.getInfo());
        }
        // 创建会话
        String sessionId = createSession(agentId, userId);

        return handleMessage(agentId, userId, sessionId, message);
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String sessionId, String message) {

        Flowable<Event> asyncResponseEvents = handleMessageStream(agentId, userId, sessionId, message);

        List<String> outputs = new ArrayList<>();
        asyncResponseEvents.blockingForEach(event -> {
            outputs.add(event.stringifyContent());
        });
        return outputs;
    }

    @Override
    public Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = armoryFactory.getAiAgentRegisterVO(agentId);
        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.CLIENT_A0301.getInfo());
        }
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Content userRequestMessage = Content.fromParts(Part.fromText(message));
        return runner.runAsync(userId, sessionId, userRequestMessage);
    }

    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) {

        Flowable<Event> asyncResponseEvents = handleMessageStream(chatCommandEntity);

        List<String> outputs = new ArrayList<>();
        asyncResponseEvents.blockingForEach(event -> {
            outputs.add(event.stringifyContent());
        });
        return outputs;
    }

    @Override
    public Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity) {
        AiAgentRegisterVO aiAgentRegisterVO = armoryFactory.getAiAgentRegisterVO(chatCommandEntity.getAgentId());
        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.CLIENT_A0301.getInfo());
        }
        // 构建 parts
        List<Part> parts = buildParts(chatCommandEntity);
        Content userContent = Content.builder().role("user").parts(parts).build();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        return runner.runAsync(chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), userContent);
    }

    private List<Part> buildParts(ChatCommandEntity chatCommandEntity) {
        List<Part> parts = new ArrayList<>(8);
        // 文本
        List<ChatCommandEntity.Content.Text> texts = chatCommandEntity.getTexts();
        if (null != texts && !texts.isEmpty()) {
            for (ChatCommandEntity.Content.Text text : texts) {
                parts.add(Part.fromText(text.getMessage()));
            }
        }
        // 文件
        List<ChatCommandEntity.Content.File> files = chatCommandEntity.getFiles();
        if (null != files && !files.isEmpty()) {
            for (ChatCommandEntity.Content.File file : files) {
                parts.add(Part.fromUri(file.getFileUri(), file.getMimeType()));
            }
        }
        // 内联数据 - 多模态
        List<ChatCommandEntity.Content.InlineData> inlineData = chatCommandEntity.getInlineData();
        if (null != inlineData && !inlineData.isEmpty()) {
            for (ChatCommandEntity.Content.InlineData data : inlineData) {
                parts.add(Part.fromBytes(data.getData(), data.getMimeType()));
            }
        }

        return parts;
    }
}

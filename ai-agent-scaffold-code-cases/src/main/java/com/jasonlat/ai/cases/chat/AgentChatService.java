package com.jasonlat.ai.cases.chat;

import com.google.adk.events.Event;
import com.jasonlat.ai.cases.chat.factory.DefaultChatFactory;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.service.IChatService;
import com.jasonlat.design.framework.tree.StrategyHandler;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jasonlat
 * 2026-04-29  22:55
 */
@Slf4j
@Service
public class AgentChatService implements IAgentChatService {

    @Resource
    private DefaultChatFactory chatFactory;

    @Resource
    private IChatService chatService;

    /**
     * 创建会话
     */
    @Override
    public String createSession(ChatCommandEntity chatCommandEntity) {
        return chatService.createSession(chatCommandEntity);
    }

    /**
     * 验证会话
     */
    @Override
    public boolean validateSession(String agentId, String userId, String sessionId) {
        return chatService.validateSession(agentId, userId, sessionId);
    }

    @Override
    public List<AiAgentConfigTableVO.AgentDefinition> queryAgentConfigList() {
        return chatService.queryAgentConfigList();
    }

    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) throws Exception {
        StrategyHandler<ChatCommandEntity, DefaultChatFactory.DynamicContext, String> strategyHandler = chatFactory.strategyHandler();
        strategyHandler.apply(chatCommandEntity, new DefaultChatFactory.DynamicContext());

        return chatService.handleMessage(chatCommandEntity);
    }

    @Override
    public Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity) throws Exception {
        StrategyHandler<ChatCommandEntity, DefaultChatFactory.DynamicContext, String> strategyHandler = chatFactory.strategyHandler();
        strategyHandler.apply(chatCommandEntity, new DefaultChatFactory.DynamicContext());

        return chatService.handleMessageStream(chatCommandEntity);
    }
}

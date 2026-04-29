package com.jasonlat.ai.cases.chat;

import com.google.adk.events.Event;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import io.reactivex.rxjava3.core.Flowable;

import java.util.List;

/**
 * @author jasonlat
 * 2026-04-29  22:54
 */
public interface IAgentChatService {

    List<String> handleMessage(ChatCommandEntity chatCommandEntity) throws Exception;

    Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity) throws Exception;

    List<AiAgentConfigTableVO.AgentDefinition> queryAgentConfigList();

    boolean validateSession(String agentId, String userId, String sessionId);

    String createSession(ChatCommandEntity build);
}

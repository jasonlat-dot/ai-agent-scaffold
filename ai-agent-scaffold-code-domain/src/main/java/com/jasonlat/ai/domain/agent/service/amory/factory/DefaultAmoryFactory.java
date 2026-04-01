package com.jasonlat.ai.domain.agent.service.amory.factory;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.service.amory.node.workflow.SequentialAgentNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author jasonlat
 * 2026-03-31  20:52
 */
@Component
public class DefaultAmoryFactory {

    /**
     * 动态上下文
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /** 智能体工作流节点 */
        private SequentialAgent sequentialAgent;

        /** LLM Api */
        private OpenAiApi openAiApi;

        /** LLM chatModel */
        private ChatModel chatModel;

        /** 智能体配置组 */
        private Map<String, BaseAgent> agentGroup = new HashMap<>(8);

        /** 智能体工作流组 */
        private List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = new ArrayList<>(8);

        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

        public List<BaseAgent> queryAgentsByName(List<String> agentNames) {
            if (agentNames == null || agentNames.isEmpty() || agentGroup.keySet().isEmpty()) {
                return Collections.emptyList();
            }
            return agentNames.stream()
                    .map(agentGroup::get)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}

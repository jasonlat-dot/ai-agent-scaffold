package com.jasonlat.ai.cases.chat.factory;

import com.jasonlat.ai.cases.chat.node.RequestValidatorNode;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jasonlat
 * 2026-04-22  20:29
 */
@Component
public class DefaultChatFactory {

    @Resource
    private RequestValidatorNode requestValidatorNode;


    public StrategyHandler<ChatCommandEntity, DynamicContext, String> strategyHandler() {
        return requestValidatorNode;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {


        private Map<String, Object> dataObjects = new HashMap<>(8);

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }
    }
}

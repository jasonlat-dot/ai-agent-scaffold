package com.jasonlat.ai.domain.agent.service.amory.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }
    }
}

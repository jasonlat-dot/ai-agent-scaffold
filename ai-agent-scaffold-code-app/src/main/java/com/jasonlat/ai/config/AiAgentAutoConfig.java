package com.jasonlat.ai.config;

import com.alibaba.fastjson2.JSON;
import com.jasonlat.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import jakarta.annotation.Resource;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * @author jasonlat
 * 2026-03-31  19:35
 */
@Data
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(AiAgentAutoConfig.class);
    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        try {
            log.info("Initializing AI agent... 【{}】", JSON.toJSONString(aiAgentAutoConfigProperties.getTables()));
        } catch (Exception e) {
            log.error("Error occurred while initializing AI agent", e);
            throw new RuntimeException(e);
        }
    }
}

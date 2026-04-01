package com.jasonlat.ai.config;

import com.alibaba.fastjson2.JSON;
import com.jasonlat.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import com.jasonlat.ai.domain.agent.service.IAmoryService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

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
    @Resource
    private IAmoryService amoryService;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        try {
            log.info("Initializing AI agent... 【{}】", JSON.toJSONString(aiAgentAutoConfigProperties.getTables()));
            // 接收智能体 - 执行装配智能体
            amoryService.acceptArmoryAgents(new ArrayList<>(aiAgentAutoConfigProperties.getTables().values()));
        } catch (Exception e) {
            log.error("Error occurred while initializing AI agent", e);
            throw new RuntimeException(e);
        }
    }
}

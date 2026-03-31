package com.jasonlat.ai.domain.agent.service.amory;

import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.service.IAmoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jasonlat
 * 2026-03-31  20:28
 */
@Slf4j
@Service
public class AmoryService implements IAmoryService {
    /**
     * 接收智能体
     *
     * @param tables 智能体配置
     */
    @Override
    public void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) {

    }
}

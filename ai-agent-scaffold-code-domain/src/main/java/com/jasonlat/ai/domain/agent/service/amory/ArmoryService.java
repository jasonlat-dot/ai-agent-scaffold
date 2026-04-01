package com.jasonlat.ai.domain.agent.service.amory;

import com.jasonlat.ai.domain.agent.model.entity.ArmoryCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.jasonlat.ai.domain.agent.service.IAmoryService;
import com.jasonlat.ai.domain.agent.service.amory.factory.DefaultArmoryFactory;
import com.jasonlat.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jasonlat
 * 2026-03-31  20:28
 */
@Slf4j
@Service
public class ArmoryService implements IAmoryService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    /**
     * 接收智能体
     *
     * @param tables 智能体配置
     */
    @Override
    public void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception {
        for (AiAgentConfigTableVO agentConfigTableVO : tables) {
            log.info("Ai Agent 配置操作 agentId: {}", agentConfigTableVO.getAgentDefinition().getAgentId());
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> rootHandler = defaultArmoryFactory.armoryStrategyHandler();
            AiAgentRegisterVO aiAgentRegisterVO = rootHandler.apply(
                    ArmoryCommandEntity.builder()
                            .aiAgentConfigTableVO(agentConfigTableVO)
                            .build(),
                    new DefaultArmoryFactory.DynamicContext());
            log.info("Ai Agent 注册结果: {}", aiAgentRegisterVO);
        }

    }
}

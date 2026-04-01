package com.jasonlat.ai.domain.agent.service.amory.node.workflow;

import com.jasonlat.ai.domain.agent.model.entity.AmoryCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.jasonlat.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import com.jasonlat.ai.domain.agent.service.amory.AbstractAmorySupport;
import com.jasonlat.ai.domain.agent.service.amory.factory.DefaultAmoryFactory;
import com.jasonlat.design.framework.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jasonlat
 * 2026-04-01  20:13
 */
@Service("parallelAgentNode")
public class ParallelAgentNode extends AbstractAmorySupport {
    /**
     * 业务流程处理方法
     * <p>
     * 子类需要实现此方法来定义具体的业务处理逻辑。
     * 该方法在异步数据加载完成后执行。
     * </p>
     *
     * @param requestParameter 请求参数
     * @param dynamicContext   动态上下文
     * @return 处理结果
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected AiAgentRegisterVO doApply(AmoryCommandEntity requestParameter, DefaultAmoryFactory.DynamicContext dynamicContext) throws Exception {
        return null;
    }

    /**
     * 获取待执行的策略处理器
     * <p>
     * 根据请求参数和动态上下文的内容，选择并返回合适的策略处理器。
     * 实现类需要根据具体的业务规则来实现策略选择逻辑。
     * </p>
     *
     * @param requestParameter 请求参数，用于确定策略选择的依据
     * @param dynamicContext   动态上下文，包含策略选择过程中需要的额外信息
     * @return 选择的策略处理器，如果没有找到合适的策略则返回null
     * @throws Exception 策略选择过程中可能抛出的异常
     */
    @Override
    public StrategyHandler<AmoryCommandEntity, DefaultAmoryFactory.DynamicContext, AiAgentRegisterVO> get(AmoryCommandEntity requestParameter, DefaultAmoryFactory.DynamicContext dynamicContext) throws Exception {
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
        if (null == agentWorkflows || agentWorkflows.isEmpty()) {
            return defaultStrategyHandler;
        }
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.get(0);
        String agentType = agentWorkflow.getType();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.formType(agentType);
        String node = agentTypeEnum.getNode();
        return switch (node) {
            case "loopAgentNode" -> beanUtils.getBean("loopAgentNode");
            case "parallelAgentNode" -> beanUtils.getBean("parallelAgentNode");
            default -> defaultStrategyHandler;
        };
    }
}

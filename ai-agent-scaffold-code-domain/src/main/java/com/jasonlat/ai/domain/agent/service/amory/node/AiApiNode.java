package com.jasonlat.ai.domain.agent.service.amory.node;

import com.jasonlat.ai.domain.agent.model.entity.AmoryCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.jasonlat.ai.domain.agent.service.amory.AbstractAmorySupport;
import com.jasonlat.ai.domain.agent.service.amory.factory.DefaultAmoryFactory;
import com.jasonlat.design.framework.tree.StrategyHandler;
import org.springframework.stereotype.Service;

/**
 * agent AiApi节点
 * @author jasonlat
 * 2026-03-31  20:59
 */
@Service
public class AiApiNode extends AbstractAmorySupport {
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
        return super.get(requestParameter, dynamicContext);
    }

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
        // 编写api实例化的操作
        return router(requestParameter, dynamicContext);
    }
}

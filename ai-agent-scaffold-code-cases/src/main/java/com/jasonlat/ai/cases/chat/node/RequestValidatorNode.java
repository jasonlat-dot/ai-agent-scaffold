package com.jasonlat.ai.cases.chat.node;

import com.jasonlat.ai.cases.chat.AbstractChatSupport;
import com.jasonlat.ai.cases.chat.factory.DefaultChatFactory;
import com.jasonlat.ai.cases.chat.node.validator.ChatRequestValidator;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.types.enums.ResponseCode;
import com.jasonlat.ai.types.exception.AppException;
import com.jasonlat.design.framework.tree.StrategyHandler;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jasonlat
 * 2026-04-29  22:52
 */
@Service
public class RequestValidatorNode extends AbstractChatSupport {

    @Resource
    private ChatRequestValidator chatRequestValidator;

    @Resource
    private SessionNode sessionNode;

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
    protected String doApply(ChatCommandEntity requestParameter, DefaultChatFactory.DynamicContext dynamicContext) throws Exception {
        chatRequestValidator.validate( requestParameter);
        return router(requestParameter, dynamicContext);
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
    public StrategyHandler<ChatCommandEntity, DefaultChatFactory.DynamicContext, String> get(ChatCommandEntity requestParameter, DefaultChatFactory.DynamicContext dynamicContext) throws Exception {
        return sessionNode;
    }
}

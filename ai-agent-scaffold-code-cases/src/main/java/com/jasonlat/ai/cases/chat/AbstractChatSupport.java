package com.jasonlat.ai.cases.chat;

import com.jasonlat.ai.cases.chat.factory.DefaultChatFactory;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.design.framework.tree.AbstractMultiThreadStrategyRouter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author jasonlat
 * 2026-04-29  22:42
 */
public abstract class AbstractChatSupport extends AbstractMultiThreadStrategyRouter<ChatCommandEntity, DefaultChatFactory.DynamicContext, String>  {
    /**
     * 多线程异步数据加载方法
     * <p>
     * 子类需要实现此方法来定义具体的异步数据加载逻辑。
     * 该方法在业务流程处理之前执行，用于预加载必要的数据。
     * </p>
     *
     * @param requestParameter 请求参数
     * @param dynamicContext   动态上下文
     * @throws ExecutionException   执行异常
     * @throws InterruptedException 中断异常
     * @throws TimeoutException     超时异常
     */
    @Override
    protected void multiThread(ChatCommandEntity requestParameter, DefaultChatFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

}

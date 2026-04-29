package com.jasonlat.ai.trigger.http;

import com.alibaba.fastjson.JSON;
import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.jasonlat.ai.domain.agent.service.IChatService;
import com.jasonlat.ai.trigger.api.IAgentService;
import com.jasonlat.ai.trigger.api.dto.AgentConfigResponse;
import com.jasonlat.ai.trigger.api.dto.ChatRequest;
import com.jasonlat.ai.trigger.api.dto.ChatResponse;
import com.jasonlat.ai.trigger.api.dto.CreateSessionRequest;
import com.jasonlat.ai.trigger.api.dto.CreateSessionResponse;
import com.jasonlat.ai.trigger.api.dto.SessionDataRequest;
import com.jasonlat.ai.trigger.api.response.Response;
import com.jasonlat.ai.trigger.http.assembler.ChatRequestAssembler;
import com.jasonlat.ai.types.enums.ResponseCode;
import com.jasonlat.ai.types.exception.AppException;
import io.reactivex.rxjava3.disposables.Disposable;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.Objects;

/**
 * @author jasonlat
 * 2026-04-04  14:32
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AgentController implements IAgentService {

    @Resource
    private IChatService chatService;

    @Resource
    private ChatRequestAssembler chatRequestAssembler;

    @Override
    @RequestMapping(value = "/validateSessionId", method = RequestMethod.POST)
    public Response<Boolean> validateSessionId(@RequestBody SessionDataRequest request) {
        try {
            Objects.requireNonNull(request.getAgentId(), "agentId cannot be null");
            Objects.requireNonNull(request.getUserId(), "userId cannot be null");
            Objects.requireNonNull(request.getSessionId(), "sessionId cannot be null");

            boolean validated = chatService.validateSession(request.getAgentId(), request.getUserId(), request.getSessionId());
            log.info("validate session agentId:{} userId:{} sessionId:{} result:{}",
                    request.getAgentId(), request.getUserId(), request.getSessionId(), validated);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(validated)
                    .build();
        } catch (AppException e) {
            log.error("validate session error", e);
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("validate session failed", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @RequestMapping(value = "/query_ai_agent_config_list", method = RequestMethod.GET)
    public Response<List<AgentConfigResponse>> queryAiAgentConfigList() {
        try {
            log.info("query agent config list start");
            List<AiAgentConfigTableVO.AgentDefinition> agentDefinitions = chatService.queryAgentConfigList();
            List<AgentConfigResponse> agentConfigResponses = agentDefinitions.stream().map(agentDefinition -> {
                AgentConfigResponse agentConfigResponse = new AgentConfigResponse();
                agentConfigResponse.setAgentId(agentDefinition.getAgentId());
                agentConfigResponse.setAgentName(agentDefinition.getAgentName());
                agentConfigResponse.setAgentDesc(agentDefinition.getAgentDesc());
                return agentConfigResponse;
            }).toList();
            log.info("query agent config list end");
            return Response.success(ResponseCode.SUCCESS.getInfo(), agentConfigResponses);
        } catch (AppException appException) {
            log.error("query agent config list error", appException);
            return Response.error(appException.getInfo());
        } catch (Exception e) {
            log.error("query agent config list failed", e);
            return Response.error(ResponseCode.UN_ERROR.getInfo());
        }
    }

    @Override
    @RequestMapping(value = "/create_session", method = RequestMethod.POST)
    public Response<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        try {
            Objects.requireNonNull(request.getAgentId(), "agentId cannot be null");
            Objects.requireNonNull(request.getUserId(), "userId cannot be null");

            log.info("create session agentId:{} userId:{}", request.getAgentId(), request.getUserId());
            String sessionId = chatService.createSession(request.getAgentId(), request.getUserId());

            CreateSessionResponse responseDTO = new CreateSessionResponse();
            responseDTO.setSessionId(sessionId);

            return Response.<CreateSessionResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("create session error", e);
            return Response.<CreateSessionResponse>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("create session failed agentId:{} userId:{}", request.getAgentId(), request.getUserId(), e);
            return Response.<CreateSessionResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @RequestMapping(value = "/chat", method = RequestMethod.POST)
    public Response<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            Objects.requireNonNull(request.getAgentId(), "agentId cannot be null");
            Objects.requireNonNull(request.getUserId(), "userId cannot be null");
            Objects.requireNonNull(request.getSessionId(), "sessionId cannot be null");
            validateChatRequest(request);

            String sessionId = request.getSessionId();
            if (StringUtils.isNotBlank(sessionId)) {
                boolean validated = chatService.validateSession(request.getAgentId(), request.getUserId(), sessionId);
                if (!validated) {
                    log.error("session validate failed agentId:{} userId:{} sessionId:{}",
                            request.getAgentId(), request.getUserId(), sessionId);
                    return Response.<ChatResponse>builder()
                            .data(null)
                            .code(ResponseCode.SESSION_NOT_EXIST.getCode())
                            .info(ResponseCode.SESSION_NOT_EXIST.getInfo())
                            .build();
                }
            }

            if (StringUtils.isBlank(sessionId)) {
                sessionId = chatService.createSession(request.getAgentId(), request.getUserId());
            }

            ChatCommandEntity chatCommandEntity = chatRequestAssembler.toChatCommand(request, sessionId);
            log.info("chat agentId:{} userId:{} sessionId:{} textCount:{} fileCount:{} inlineCount:{}",
                    request.getAgentId(), request.getUserId(), sessionId,
                    sizeOf(chatCommandEntity.getTexts()), sizeOf(chatCommandEntity.getFiles()), sizeOf(chatCommandEntity.getInlineData()));

            List<String> messages = chatService.handleMessage(chatCommandEntity);
            ChatResponse responseDTO = new ChatResponse();
            responseDTO.setContent(String.join("\n", messages));

            return Response.<ChatResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("chat error", e);
            return Response.<ChatResponse>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("chat failed agentId:{} userId:{}", request.getAgentId(), request.getUserId(), e);
            return Response.<ChatResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @RequestMapping(value = "/chat_stream", method = RequestMethod.POST)
    public ResponseBodyEmitter chatStream(@RequestBody ChatRequest request, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
        try {
            Objects.requireNonNull(request.getAgentId(), "agentId cannot be null");
            Objects.requireNonNull(request.getUserId(), "userId cannot be null");
            Objects.requireNonNull(request.getSessionId(), "sessionId cannot be null");
            validateChatRequest(request);

            String sessionId = request.getSessionId();
            if (StringUtils.isNotBlank(sessionId)) {
                boolean validated = chatService.validateSession(request.getAgentId(), request.getUserId(), sessionId);
                if (!validated) {
                    log.error("session validate failed agentId:{} userId:{} sessionId:{}",
                            request.getAgentId(), request.getUserId(), sessionId);
                    emitter.send("data: error: " + ResponseCode.SESSION_NOT_EXIST.getInfo() + "\n\n");
                    emitter.complete();
                    return emitter;
                }
            }

            if (StringUtils.isBlank(sessionId)) {
                sessionId = chatService.createSession(request.getAgentId(), request.getUserId());
            }

            ChatCommandEntity chatCommandEntity = chatRequestAssembler.toChatCommand(request, sessionId);
            log.info("chat stream agentId:{} userId:{} sessionId:{} textCount:{} fileCount:{} inlineCount:{}",
                    request.getAgentId(), request.getUserId(), sessionId,
                    sizeOf(chatCommandEntity.getTexts()), sizeOf(chatCommandEntity.getFiles()), sizeOf(chatCommandEntity.getInlineData()));

            Disposable subscribe = chatService.handleMessageStream(chatCommandEntity)
                    .subscribe(
                            event -> {
                                try {
                                    String content = JSON.toJSONString(event.stringifyContent());
                                    if (!content.isEmpty()) {
                                        emitter.send("data: " + content + "\n\n");
                                    }
                                } catch (Exception e) {
                                    log.error("stream send failed", e);
                                }
                            },
                            emitter::completeWithError,
                            () -> {
                                try {
                                    emitter.send("data: [DONE]\n\n");
                                } catch (Exception e) {
                                    log.error("stream done marker failed", e);
                                }
                                emitter.complete();
                            }
                    );
            emitter.onCompletion(subscribe::dispose);
        } catch (AppException e) {
            log.error("chat stream error", e);
            try {
                emitter.send("data: error: " + JSON.toJSONString(e.getInfo()) + "\n\n");
            } catch (Exception ex) {
                log.error("stream send error payload failed", ex);
            }
            emitter.complete();
        } catch (Exception e) {
            log.error("chat stream failed", e);
            try {
                emitter.send("data: error: " + JSON.toJSONString(e.getMessage()) + "\n\n");
            } catch (Exception ex) {
                log.error("stream send exception message failed", ex);
            }
            emitter.complete();
        }
        return emitter;
    }

    private void validateChatRequest(ChatRequest request) {
        if (!chatRequestAssembler.hasInputContent(request)) {
            throw new AppException(ResponseCode.CLIENT_A0410.getCode(), "texts/files/inlineData requires at least one item");
        }
    }

    private int sizeOf(List<?> items) {
        return items == null ? 0 : items.size();
    }
}

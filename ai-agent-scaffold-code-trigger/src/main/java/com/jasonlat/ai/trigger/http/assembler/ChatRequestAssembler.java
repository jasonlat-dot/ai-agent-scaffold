package com.jasonlat.ai.trigger.http.assembler;

import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.trigger.api.dto.ChatRequest;
import com.jasonlat.ai.types.enums.ResponseCode;
import com.jasonlat.ai.types.exception.AppException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Assemble trigger-layer chat requests into domain chat commands.
 *
 * @author jasonlat
 * 2026-04-29  22:10
 */
@Component
public class ChatRequestAssembler {

    public boolean hasInputContent(ChatRequest request) {
        boolean hasText = request.getTexts() != null &&
                request.getTexts().stream().anyMatch(item -> StringUtils.isNotBlank(item.getMessage()));
        boolean hasFiles = request.getFiles() != null && request.getFiles().stream()
                .anyMatch(item -> StringUtils.isNotBlank(item.getFileUri()) && StringUtils.isNotBlank(item.getMimeType()));
        boolean hasInlineData = request.getInlineData() != null && request.getInlineData().stream()
                .anyMatch(item -> StringUtils.isNotBlank(item.getData()));
        return hasText || hasFiles || hasInlineData;
    }

    public ChatCommandEntity toChatCommand(ChatRequest request, String sessionId) {
        return ChatCommandEntity.builder()
                .agentId(request.getAgentId())
                .userId(request.getUserId())
                .sessionId(sessionId)
                .texts(buildTexts(request))
                .files(buildFiles(request))
                .inlineData(buildInlineData(request))
                .build();
    }

    private List<ChatCommandEntity.Content.Text> buildTexts(ChatRequest request) {
        List<ChatCommandEntity.Content.Text> texts = new ArrayList<>();
        if (request.getTexts() != null) {
            request.getTexts().stream()
                    .filter(item -> StringUtils.isNotBlank(item.getMessage()))
                    .map(item -> ChatCommandEntity.Content.Text.builder().message(item.getMessage()).build())
                    .forEach(texts::add);
        }
        return texts.isEmpty() ? null : texts;
    }

    private List<ChatCommandEntity.Content.File> buildFiles(ChatRequest request) {
        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            return null;
        }

        List<ChatCommandEntity.Content.File> files = request.getFiles().stream()
                .filter(item -> StringUtils.isNotBlank(item.getFileUri()) && StringUtils.isNotBlank(item.getMimeType()))
                .map(item -> ChatCommandEntity.Content.File.builder()
                        .fileUri(item.getFileUri())
                        .mimeType(item.getMimeType())
                        .build())
                .toList();
        return files.isEmpty() ? null : files;
    }

    private List<ChatCommandEntity.Content.InlineData> buildInlineData(ChatRequest request) {
        if (request.getInlineData() == null || request.getInlineData().isEmpty()) {
            return null;
        }

        List<ChatCommandEntity.Content.InlineData> inlineData = new ArrayList<>();
        for (ChatRequest.InlineDataContent item : request.getInlineData()) {
            if (StringUtils.isBlank(item.getData())) {
                continue;
            }
            inlineData.add(ChatCommandEntity.Content.InlineData.builder()
                    .data(decodeBase64(item.getData()))
                    .mimeType(resolveMimeType(item))
                    .build());
        }
        return inlineData.isEmpty() ? null : inlineData;
    }

    private byte[] decodeBase64(String data) {
        String raw = data;
        int commaIndex = data.indexOf(',');
        if (data.startsWith("data:") && commaIndex >= 0) {
            raw = data.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            throw new AppException(ResponseCode.CLIENT_A0427.getCode(), "inlineData base64 decode failed", e);
        }
    }

    private String resolveMimeType(ChatRequest.InlineDataContent item) {
        if (StringUtils.isNotBlank(item.getMimeType())) {
            return item.getMimeType();
        }
        String data = item.getData();
        if (StringUtils.isNotBlank(data) && data.startsWith("data:")) {
            int semicolonIndex = data.indexOf(';');
            if (semicolonIndex > 5) {
                return data.substring(5, semicolonIndex);
            }
        }
        throw new AppException(ResponseCode.CLIENT_A0410.getCode(), "inlineData.mimeType cannot be null");
    }
}

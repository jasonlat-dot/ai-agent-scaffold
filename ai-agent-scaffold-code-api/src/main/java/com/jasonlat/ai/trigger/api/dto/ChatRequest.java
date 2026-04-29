package com.jasonlat.ai.trigger.api.dto;

import lombok.Data;

import java.util.List;

/**
 * @author jasonlat
 * 2026-04-04  14:26
 */
@Data
public class ChatRequest {

    private String agentId;

    private String userId;

    private String sessionId;

    private List<TextContent> texts;

    private List<FileContent> files;

    private List<InlineDataContent> inlineData;

    @Data
    public static class TextContent {
        private String message;
    }

    @Data
    public static class FileContent {
        private String fileUri;
        private String mimeType;
    }

    @Data
    public static class InlineDataContent {
        private String data;
        private String mimeType;
        private String fileName;
    }
}

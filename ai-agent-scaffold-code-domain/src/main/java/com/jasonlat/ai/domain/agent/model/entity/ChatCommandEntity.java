package com.jasonlat.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话命令实体
 * @author jasonlat
 * 2026-04-04  10:33
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCommandEntity {

    private String agentId;

    private String userId;

    private String sessionId;

    private List<Content.Text> texts;

    private List<Content.File> files;

    private List<Content.InlineData> inlineData;

    @Data
    public static class Content {
        /**
         * 文本内容
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Text {
            private String message;
        }
        /**
         * 文件 - uri形式
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class File {
            private String fileUri;
            private String mimeType;
        }
        /**
         * 文件 - 字节数组
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InlineData {
            private byte[] data;
            private String mimeType;
        }
    }
}

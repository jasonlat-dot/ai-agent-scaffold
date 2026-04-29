package com.jasonlat.ai.cases.chat.node.validator;

import com.jasonlat.ai.domain.agent.model.entity.ChatCommandEntity;
import com.jasonlat.ai.types.enums.ResponseCode;
import com.jasonlat.ai.types.exception.AppException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validate chat requests before converting them into domain commands.
 *
 * @author jasonlat
 * 2026-04-29  23:05
 */
@Component
public class ChatRequestValidator {

    private static final int MAX_INLINE_DATA_BYTES = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_INLINE_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/svg+xml",
            "application/pdf"
    );

    public void validate(ChatCommandEntity request) {
        Objects.requireNonNull(request.getAgentId(), "agentId cannot be null");
        Objects.requireNonNull(request.getUserId(), "userId cannot be null");

        boolean hasTexts = request.getTexts() != null &&
                request.getTexts().stream().anyMatch(item -> StringUtils.isNotBlank(item.getMessage()));
        boolean hasFiles = request.getFiles() != null &&
                request.getFiles().stream().anyMatch(item ->
                        StringUtils.isNotBlank(item.getFileUri()) && StringUtils.isNotBlank(item.getMimeType()));
        boolean hasInlineData = request.getInlineData() != null && !request.getInlineData().isEmpty();

        if (!hasTexts && !hasFiles && !hasInlineData) {
            throw new AppException(ResponseCode.CLIENT_A0410.getCode(), "texts/files/inlineData requires at least one item");
        }

        validateInlineData(request.getInlineData());
    }


    private void validateInlineData(List<ChatCommandEntity.Content.InlineData> inlineDataList) {
        if (inlineDataList == null || inlineDataList.isEmpty()) {
            return;
        }

        for (ChatCommandEntity.Content.InlineData inlineData : inlineDataList) {
            byte[] bytes = inlineData.getData();
            if (StringUtils.isBlank(inlineData.getMimeType())) {
                throw new AppException(ResponseCode.CLIENT_A0410.getCode(), "inlineData.mimeType cannot be null");
            }
            if (!ALLOWED_INLINE_MIME_TYPES.contains(inlineData.getMimeType())) {
                throw new AppException(ResponseCode.CLIENT_A0410.getCode(), "inlineData.mimeType is not allowed: " + inlineData.getMimeType());
            }
            if (bytes.length == 0) {
                throw new AppException(ResponseCode.CLIENT_A0410.getCode(), "inlineData bytes cannot be empty");
            }
            if (bytes.length > MAX_INLINE_DATA_BYTES) {
                throw new AppException(ResponseCode.CLIENT_A0702.getCode(), "inlineData size exceeds limit");
            }
            if (!matchesFileSignature(bytes, inlineData.getMimeType())) {
                throw new AppException(ResponseCode.CLIENT_A0701.getCode(), "inlineData content does not match mimeType: " + inlineData.getMimeType());
            }
        }
    }



    private boolean matchesFileSignature(byte[] bytes, String mimeType) {
        return switch (mimeType) {
            case "image/png" -> startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            case "image/jpeg" -> startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/gif" -> startsWith(bytes, "GIF8".getBytes(StandardCharsets.US_ASCII));
            case "image/webp" -> startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII)) &&
                    containsAt(bytes, "WEBP".getBytes(StandardCharsets.US_ASCII), 8);
            case "image/bmp" -> startsWith(bytes, "BM".getBytes(StandardCharsets.US_ASCII));
            case "application/pdf" -> startsWith(bytes, "%PDF".getBytes(StandardCharsets.US_ASCII));
            case "image/svg+xml" -> {
                String content = new String(bytes, StandardCharsets.UTF_8).trim();
                yield content.startsWith("<svg") || content.startsWith("<?xml") || content.contains("<svg");
            }
            default -> false;
        };
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (source[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAt(byte[] source, byte[] target, int offset) {
        if (source.length < offset + target.length) {
            return false;
        }
        for (int index = 0; index < target.length; index++) {
            if (source[offset + index] != target[index]) {
                return false;
            }
        }
        return true;
    }
}

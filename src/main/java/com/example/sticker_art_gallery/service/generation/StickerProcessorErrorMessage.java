package com.example.sticker_art_gallery.service.generation;

import java.util.Map;

public final class StickerProcessorErrorMessage {

    private StickerProcessorErrorMessage() {
    }

    @SuppressWarnings("unchecked")
    public static String extractHumanMessage(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        Object detail = payload.get("detail");
        if (detail instanceof Map<?, ?> detailMapRaw) {
            Map<String, Object> detailMap = (Map<String, Object>) detailMapRaw;
            Object message = detailMap.get("message");
            if (message != null) {
                String text = message.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        if (detail instanceof String detailText) {
            String text = detailText.trim();
            if (!text.isEmpty()) {
                return text;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static String extractDetailCode(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        Object detail = payload.get("detail");
        if (detail instanceof Map<?, ?> detailMapRaw) {
            Map<String, Object> detailMap = (Map<String, Object>) detailMapRaw;
            Object code = detailMap.get("code");
            if (code != null) {
                String text = code.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return null;
    }

    public static String fallbackForStatus(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Invalid file_id format (expected ws_...)";
            case 404 -> "Generation task not found";
            case 410 -> "Generation task expired (TTL)";
            case 422 -> "Generation request validation failed";
            case 424 -> "Generation failed in upstream provider";
            default -> "Terminal status: " + statusCode;
        };
    }

    public static String humanMessageOrFallback(Map<String, Object> payload, int statusCode) {
        String extracted = extractHumanMessage(payload);
        return extracted != null ? extracted : fallbackForStatus(statusCode);
    }

    public static boolean isBackgroundRemovalFailure(Map<String, Object> payload) {
        String code = extractDetailCode(payload);
        if (code != null) {
            String normalized = code.trim().toLowerCase().replace('-', '_');
            if ("background_removal_failed".equals(normalized)
                    || "background_remover_failed".equals(normalized)
                    || "stickerprocessorbackgroundremoverfailed".equals(normalized)) {
                return true;
            }
        }

        String message = extractHumanMessage(payload);
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.trim().toLowerCase();
        // sticker-processor / WaveSpeed иногда пишут "remover", иногда "removal"
        return normalizedMessage.contains("background removal failed")
                || normalizedMessage.contains("background remover failed")
                || normalizedMessage.contains("backgroundremoverfailed")
                || normalizedMessage.contains("stickerprocessorbackgroundremoverfailed")
                || (normalizedMessage.contains("background remover") && normalizedMessage.contains("fail"));
    }
}

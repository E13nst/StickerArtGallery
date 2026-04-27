package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerV2Request;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StickerProcessorGenerationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerProcessorGenerationClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String stickerProcessorUrl;

    public StickerProcessorGenerationClient(
            RestTemplate restTemplate,
            @Value("${sticker.processor.url}") String stickerProcessorUrl) {
        this.restTemplate = restTemplate;
        this.stickerProcessorUrl = stickerProcessorUrl;
        this.objectMapper = new ObjectMapper();
    }

    public SubmitResult submitGenerate(GenerateStickerV2Request request) {
        return submitGenerate(request, null);
    }

    /**
     * Отправляет запрос на генерацию в sticker-processor.
     *
     * @param sourceImageUrls независимый от {@code request.imageIds} список публичных URL источников для img2img/edit.
     *                        Используется для synthetic id вида {@code img_sagref_*} (кэш StickerArtGallery), которых
     *                        нет в Redis sticker-processor: для них передаём только URL, без id. {@code source_image_ids}
     *                        и {@code source_image_urls} в payload — два независимых канала (max 4 combined).
     */
    public SubmitResult submitGenerate(GenerateStickerV2Request request, List<String> sourceImageUrls) {
        String url = stickerProcessorUrl + "/stickers/wavespeed/generate";
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", request.getPrompt());
        payload.put("model", request.getModel());
        payload.put("size", request.getSize());
        payload.put("seed", request.getSeed());
        payload.put("num_images", request.getNumImages());
        payload.put("strength", request.getStrength());
        payload.put("remove_background", request.getRemoveBackground());
        List<String> sourceImageIds = request.getImageIds();
        if (sourceImageIds != null && !sourceImageIds.isEmpty()) {
            payload.put("source_image_ids", sourceImageIds);
        }
        if (sourceImageUrls != null && !sourceImageUrls.isEmpty()) {
            payload.put("source_image_urls", sourceImageUrls);
        }

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, jsonHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            Map<String, Object> data = toMap(response.getBody());
            return new SubmitResult(
                    data.get("file_id") != null ? data.get("file_id").toString() : null,
                    data.get("status") != null ? data.get("status").toString() : null,
                    data.get("provider_request_id") != null ? data.get("provider_request_id").toString() : null
            );
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            String raw = e.getResponseBodyAsString();
            Map<String, Object> body = toMap(raw);
            String extracted = StickerProcessorErrorMessage.extractHumanMessage(body);
            String reason;
            if (extracted != null && !extracted.isBlank()) {
                reason = extracted;
            } else if (raw != null && !raw.isBlank()) {
                int cap = 800;
                reason = "HTTP " + statusCode + ": " + (raw.length() > cap ? raw.substring(0, cap) + "…" : raw);
            } else {
                reason = StickerProcessorErrorMessage.fallbackForStatus(statusCode);
            }
            throw new RuntimeException("STICKER_PROCESSOR submit failed: " + reason, e);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("STICKER_PROCESSOR submit timeout/network error: " + e.getMessage(), e);
        }
    }

    public PollResult pollResult(String fileId) {
        String url = stickerProcessorUrl + "/stickers/wavespeed/" + fileId;
        try {
            HttpEntity<Void> entity = new HttpEntity<>(jsonHeaders());
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            MediaType contentType = response.getHeaders().getContentType();
            byte[] body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()
                    && contentType != null
                    && MediaType.valueOf("image/webp").isCompatibleWith(contentType)
                    && body != null
                    && body.length > 0) {
                return PollResult.imageReady(body);
            }

            if (body != null && body.length > 0) {
                Map<String, Object> json = toMap(new String(body, StandardCharsets.UTF_8));
                return PollResult.jsonStatus(response.getStatusCode().value(), json);
            }
            return PollResult.jsonStatus(response.getStatusCode().value(), Map.of());
        } catch (HttpStatusCodeException e) {
            Map<String, Object> json = toMap(e.getResponseBodyAsString());
            return PollResult.jsonStatus(e.getStatusCode().value(), json);
        } catch (ResourceAccessException e) {
            LOGGER.warn("STICKER_PROCESSOR poll timeout/network error: {}", e.getMessage());
            return PollResult.jsonStatus(0, Map.of("detail", "network_error"));
        }
    }

    public SaveResult saveToSet(String fileId, Long userId, String name, String title, String emoji, Integer waitTimeoutSec) {
        String url = stickerProcessorUrl + "/stickers/wavespeed/save-to-set";
        Map<String, Object> payload = new HashMap<>();
        payload.put("file_id", fileId);
        payload.put("user_id", userId);
        payload.put("name", name);
        if (title != null && !title.isBlank()) {
            payload.put("title", title);
        }
        payload.put("emoji", emoji);
        payload.put("wait_timeout_sec", waitTimeoutSec);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, jsonHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            Map<String, Object> body = toMap(response.getBody());
            return new SaveResult(response.getStatusCode().value(), body);
        } catch (HttpStatusCodeException e) {
            return new SaveResult(e.getStatusCode().value(), toMap(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            return new SaveResult(0, Map.of("detail", "network_error"));
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.valueOf("image/webp")));
        return headers;
    }

    private Map<String, Object> toMap(String body) {
        if (body == null || body.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public record SubmitResult(String fileId, String status, String providerRequestId) {}
    public record SaveResult(int httpStatus, Map<String, Object> payload) {}

    public static class PollResult {
        private final boolean imageReady;
        private final byte[] imageBytes;
        private final int httpStatus;
        private final Map<String, Object> payload;

        private PollResult(boolean imageReady, byte[] imageBytes, int httpStatus, Map<String, Object> payload) {
            this.imageReady = imageReady;
            this.imageBytes = imageBytes;
            this.httpStatus = httpStatus;
            this.payload = payload;
        }

        public static PollResult imageReady(byte[] imageBytes) {
            return new PollResult(true, imageBytes, 200, Map.of());
        }

        public static PollResult jsonStatus(int httpStatus, Map<String, Object> payload) {
            return new PollResult(false, null, httpStatus, payload);
        }

        public boolean isImageReady() {
            return imageReady;
        }

        public byte[] getImageBytes() {
            return imageBytes;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }
    }
}

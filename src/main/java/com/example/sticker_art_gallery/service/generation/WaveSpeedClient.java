package com.example.sticker_art_gallery.service.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class WaveSpeedClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaveSpeedClient.class);
    private static final int MAX_RETRIES = 2;
    private static final Random random = new Random();

    private final String apiKey;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WaveSpeedClient(
            @Value("${wavespeed.api-key}") String apiKey,
            @Value("${wavespeed.base-url:https://api.wavespeed.ai/api/v3}") String baseUrl,
            @Value("${wavespeed.timeout.connect:5000}") int connectTimeout,
            @Value("${wavespeed.timeout.read:20000}") int readTimeout) {
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("WAVESPEED_API_KEY is required");
        }
        
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        
        // Создаем RestTemplate с кастомными таймаутами
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
        
        LOGGER.info("WaveSpeedClient initialized with API key: {}...", apiKey.substring(0, Math.min(4, apiKey.length())));
    }

    public String submitFluxSchnell(
            String finalPrompt,
            String size,
            String outputFormat,
            Integer seed,
            Integer numImages,
            Double strength,
            String image) {
        
        String url = baseUrl + "/wavespeed-ai/flux-schnell";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("enable_base64_output", false);
        payload.put("enable_sync_mode", false);
        payload.put("image", image != null ? image : "");
        payload.put("num_images", numImages != null ? numImages : 1);
        payload.put("output_format", outputFormat != null ? outputFormat : "png");
        payload.put("prompt", finalPrompt);
        payload.put("seed", seed != null ? seed : -1);
        payload.put("size", size != null ? size : "512*512");
        payload.put("strength", strength != null ? strength : 0.8);
        
        LOGGER.info("WaveSpeed: Submitting flux-schnell request to {}", url);
        LOGGER.debug("WaveSpeed: Payload: prompt_length={}, size={}, output_format={}, seed={}, num_images={}",
                finalPrompt.length(), size, outputFormat, seed, numImages);
        
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    double waitTime = Math.pow(2, attempt) + random.nextDouble();
                    LOGGER.info("WaveSpeed: Retry attempt {}/{} for flux-schnell, waiting {:.1f}s",
                            attempt + 1, MAX_RETRIES + 1, waitTime);
                    Thread.sleep((long) (waitTime * 1000));
                }
                
                HttpHeaders headers = createHeaders();
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                LOGGER.debug("WaveSpeed: Response status: {}", response.getStatusCode());
                
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new HttpServerErrorException(response.getStatusCode(), "WaveSpeed API error");
                }
                
                JsonNode data = objectMapper.readTree(response.getBody());
                LOGGER.debug("WaveSpeed: Response data keys: {}", data.fieldNames());
                
                String requestId = extractRequestId(data);
                if (requestId == null) {
                    LOGGER.error("WaveSpeed: Invalid response structure - no id found. Full response: {}", response.getBody());
                    throw new IllegalArgumentException("Invalid response from WaveSpeed API: " + response.getBody());
                }
                
                LOGGER.info("WaveSpeed: Flux-schnell task submitted successfully: request_id={}", requestId);
                return requestId;
                
            } catch (HttpServerErrorException | HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 || 
                    (e.getStatusCode().is5xxServerError() && attempt < MAX_RETRIES)) {
                    lastException = e;
                    continue;
                }
                throw new RuntimeException("WaveSpeed API error: " + e.getMessage(), e);
            } catch (ResourceAccessException e) {
                if (attempt < MAX_RETRIES) {
                    lastException = e;
                    continue;
                }
                throw new RuntimeException("WaveSpeed network error: " + e.getMessage(), e);
            } catch (Exception e) {
                if (attempt < MAX_RETRIES && (e.getMessage() == null || e.getMessage().contains("timeout"))) {
                    lastException = e;
                    continue;
                }
                throw new RuntimeException("WaveSpeed error: " + e.getMessage(), e);
            }
        }
        
        if (lastException != null) {
            throw new RuntimeException("WaveSpeed error after retries: " + lastException.getMessage(), lastException);
        }
        throw new RuntimeException("WaveSpeed error: failed after retries");
    }

    public Map<String, Object> getPredictionResult(String requestId) {
        String url = baseUrl + "/predictions/" + requestId + "/result";
        
        LOGGER.debug("WaveSpeed: Getting prediction result from {}", url);
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, String.class);
            
            LOGGER.debug("WaveSpeed: GET {} -> Status: {}", url, response.getStatusCode());
            
            if (response.getStatusCode().value() == 404) {
                LOGGER.warn("WaveSpeed prediction not found: request_id={}", requestId);
                return null;
            }
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("WaveSpeed API error {}: {}", response.getStatusCode(), response.getBody());
                return null;
            }
            
            JsonNode data = objectMapper.readTree(response.getBody());
            LOGGER.debug("WaveSpeed: Response data keys: {}", data.fieldNames());
            
            // Проверяем структуру ответа
            JsonNode innerData = data.has("data") && data.get("data").isObject() ? data.get("data") : data;
            String status = innerData.has("status") ? innerData.get("status").asText() : "unknown";
            Long executionTime = innerData.has("executionTime") ? innerData.get("executionTime").asLong() : null;
            JsonNode outputsNode = innerData.has("outputs") ? innerData.get("outputs") : null;
            List<String> outputs = null;
            if (outputsNode != null && outputsNode.isArray()) {
                outputs = objectMapper.convertValue(outputsNode, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
            
            LOGGER.info("WaveSpeed: Result for {}: status={}, executionTime={}, outputs_count={}",
                    requestId, status, executionTime, outputs != null ? outputs.size() : 0);
            
            if ("completed".equalsIgnoreCase(status) && outputs != null && !outputs.isEmpty()) {
                LOGGER.info("WaveSpeed: Completed! First output URL: {}...", 
                        outputs.get(0).substring(0, Math.min(80, outputs.get(0).length())));
            } else if ("failed".equalsIgnoreCase(status)) {
                String errorMsg = innerData.has("error") ? innerData.get("error").asText() : "Unknown error";
                LOGGER.warn("WaveSpeed: Generation failed for {}: {}", requestId, errorMsg);
            }
            
            // Возвращаем как Map для совместимости
            Map<String, Object> result = new HashMap<>();
            result.put("status", status);
            if (executionTime != null) {
                result.put("executionTime", executionTime);
            }
            if (outputs != null) {
                result.put("outputs", outputs);
            }
            if (innerData.has("error")) {
                result.put("error", innerData.get("error").asText());
            }
            if (data.has("data")) {
                result.put("data", objectMapper.convertValue(data.get("data"), Map.class));
            }
            
            return result;
            
        } catch (ResourceAccessException e) {
            LOGGER.error("WaveSpeed network error: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("WaveSpeed error getting result: {}", e.getMessage(), e);
            return null;
        }
    }

    public String submitBackgroundRemover(String imageUrl) {
        String url = baseUrl + "/wavespeed-ai/image-background-remover";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("enable_base64_output", false);
        payload.put("enable_sync_mode", false);
        payload.put("image", imageUrl);
        
        String logUrl = extractLogUrl(imageUrl);
        LOGGER.info("WaveSpeed: Submitting background-remover request to {}", url);
        LOGGER.debug("WaveSpeed: Image URL: {}", logUrl);
        
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    double waitTime = Math.pow(2, attempt) + random.nextDouble();
                    LOGGER.info("WaveSpeed: Retry attempt {}/{} for bg-remover, waiting {:.1f}s",
                            attempt + 1, MAX_RETRIES + 1, waitTime);
                    Thread.sleep((long) (waitTime * 1000));
                }
                
                HttpHeaders headers = createHeaders();
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                LOGGER.debug("WaveSpeed: Response status: {}", response.getStatusCode());
                
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new HttpServerErrorException(response.getStatusCode(), "WaveSpeed API error");
                }
                
                JsonNode data = objectMapper.readTree(response.getBody());
                LOGGER.debug("WaveSpeed: Response data keys: {}", data.fieldNames());
                
                String requestId = extractRequestId(data);
                if (requestId == null) {
                    LOGGER.error("WaveSpeed: Invalid response structure - no id found. Full response: {}", response.getBody());
                    throw new IllegalArgumentException("Invalid response from WaveSpeed API: " + response.getBody());
                }
                
                LOGGER.info("WaveSpeed: Background-remover task submitted successfully: request_id={}, image={}", 
                        requestId, logUrl);
                return requestId;
                
            } catch (HttpServerErrorException | HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 || 
                    (e.getStatusCode().is5xxServerError() && attempt < MAX_RETRIES)) {
                    lastException = e;
                    continue;
                }
                throw new RuntimeException("WaveSpeed API error: " + e.getMessage(), e);
            } catch (ResourceAccessException e) {
                if (attempt < MAX_RETRIES) {
                    lastException = e;
                    continue;
                }
                throw new RuntimeException("WaveSpeed network error: " + e.getMessage(), e);
            } catch (Exception e) {
                if (attempt < MAX_RETRIES && (e.getMessage() == null || e.getMessage().contains("timeout"))) {
                    lastException = e;
                    continue;
                }
                throw new RuntimeException("WaveSpeed error: " + e.getMessage(), e);
            }
        }
        
        if (lastException != null) {
            throw new RuntimeException("WaveSpeed error after retries: " + lastException.getMessage(), lastException);
        }
        throw new RuntimeException("WaveSpeed error: failed after retries");
    }

    public byte[] downloadImage(String imageUrl, int maxSize) {
        String logUrl = extractLogUrl(imageUrl);
        LOGGER.debug("WaveSpeed: Downloading image from {}", logUrl);
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl, HttpMethod.GET, requestEntity, byte[].class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("WaveSpeed: HTTP error {} downloading image from {}", 
                        response.getStatusCode(), logUrl);
                return null;
            }
            
            byte[] imageBytes = response.getBody();
            if (imageBytes == null) {
                LOGGER.warn("WaveSpeed: Empty response body for {}", logUrl);
                return null;
            }
            
            if (imageBytes.length > maxSize) {
                    LOGGER.warn("WaveSpeed: Image size {} exceeds max_size {} for {}", 
                            imageBytes.length, maxSize, logUrl);
                return null;
            }
            
            LOGGER.debug("WaveSpeed: Successfully downloaded image from {}, size: {} bytes", 
                    logUrl, imageBytes.length);
            return imageBytes;
            
        } catch (ResourceAccessException e) {
            LOGGER.error("WaveSpeed: Network error downloading image from {}: {}", logUrl, e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("WaveSpeed: Unexpected error downloading image from {}: {}", logUrl, e.getMessage(), e);
            return null;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private String extractRequestId(JsonNode data) {
        // Поддержка нового формата с вложенным data
        if (data.has("data") && data.get("data").isObject()) {
            JsonNode innerData = data.get("data");
            if (innerData.has("id")) {
                return innerData.get("id").asText();
            }
            if (innerData.has("requestId")) {
                return innerData.get("requestId").asText();
            }
        }
        // Fallback на старый формат
        if (data.has("id")) {
            return data.get("id").asText();
        }
        if (data.has("requestId")) {
            return data.get("requestId").asText();
        }
        return null;
    }

    private String extractLogUrl(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();
            String lastSegment = path.substring(path.lastIndexOf('/') + 1);
            return uri.getHost() + "/" + lastSegment;
        } catch (Exception e) {
            return "image_url";
        }
    }
}

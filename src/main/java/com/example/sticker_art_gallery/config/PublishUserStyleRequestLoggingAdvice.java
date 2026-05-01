package com.example.sticker_art_gallery.config;

import com.example.sticker_art_gallery.dto.generation.PublishUserStyleFromTaskRequest;
import com.example.sticker_art_gallery.dto.generation.PublishUserStylePayloadKeyFlavor;
import com.example.sticker_art_gallery.dto.generation.PublishUserStylePayloadKeyFlavorDetector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Логирует и считает метрику формата ключей (camel/snake/mixed) для {@code PublishUserStyleFromTaskRequest}
 * до того, как Jackson десериализует тело.
 */
@ControllerAdvice
public class PublishUserStyleRequestLoggingAdvice extends RequestBodyAdviceAdapter {

    private static final String METRIC = "generation.publish.user.style.payload.key.flavor";

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishUserStyleRequestLoggingAdvice.class);

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistryOrNull;

    public PublishUserStyleRequestLoggingAdvice(ObjectMapper objectMapper,
                                                ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.objectMapper = objectMapper;
        this.meterRegistryOrNull = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return PublishUserStyleFromTaskRequest.class.equals(methodParameter.getParameterType());
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
        classifyAndObserve(body);
        return new RepeatedReadHttpInputMessage(inputMessage.getHeaders(), body);
    }

    private void classifyAndObserve(byte[] body) {
        PublishUserStylePayloadKeyFlavor flavor = PublishUserStylePayloadKeyFlavor.UNKNOWN;
        try {
            if (body.length == 0) {
                LOGGER.info("publish-user-style входной JSON: keyFlavor={}, пустое тело", flavor);
                bumpMetric(flavor);
                return;
            }
            JsonNode root = objectMapper.readTree(body);
            flavor = PublishUserStylePayloadKeyFlavorDetector.detect(root);
            LOGGER.info("publish-user-style входной JSON: keyFlavor={}", flavor);
            bumpMetric(flavor);
        } catch (Exception e) {
            LOGGER.warn("publish-user-style: не удалось классифицировать ключи тела запроса: {}", e.toString());
            bumpMetric(PublishUserStylePayloadKeyFlavor.UNKNOWN);
        }
    }

    private void bumpMetric(PublishUserStylePayloadKeyFlavor flavor) {
        if (meterRegistryOrNull == null) {
            return;
        }
        meterRegistryOrNull.counter(METRIC, "flavor", flavor.name()).increment();
    }

    private static final class RepeatedReadHttpInputMessage implements HttpInputMessage {
        private final HttpHeaders headers;
        private final byte[] body;

        private RepeatedReadHttpInputMessage(HttpHeaders headers, byte[] body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}

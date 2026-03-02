package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageResponse;
import com.example.sticker_art_gallery.exception.BotException;
import com.example.sticker_art_gallery.model.messaging.MessageAuditEventStatus;
import com.example.sticker_art_gallery.model.messaging.MessageAuditStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(StickerBotMessageServiceRetryTest.TestConfig.class)
@DisplayName("Retry-тесты StickerBotMessageService")
class StickerBotMessageServiceRetryTest {

    private static final String API_URL = "https://stickerbot.example.com";
    private static final Long USER_ID = 123456789L;
    private static final String MESSAGE_TEXT = "Hello from retry test";

    @jakarta.annotation.Resource
    private StickerBotMessageService service;

    @jakarta.annotation.Resource
    private RestTemplate restTemplate;

    @jakarta.annotation.Resource
    private MessageAuditService messageAuditService;

    @BeforeEach
    void setUp() {
        reset(restTemplate, messageAuditService);
    }

    @Test
    @DisplayName("Успех на 3-й попытке после двух 5xx ошибок")
    void sendToUser_successAfterRetries() {
        SendBotMessageResponse success = new SendBotMessageResponse("sent", 100L, 200L, "plain");
        when(restTemplate.exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway #1"))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway #2"))
                .thenReturn(ResponseEntity.ok(success));

        SendBotMessageResponse result = service.sendPlainTextToUser(USER_ID, MESSAGE_TEXT);

        assertThat(result).isNotNull();
        assertThat(result.isSent()).isTrue();
        verify(restTemplate, times(3)).exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)
        );
        verify(messageAuditService, atLeast(2)).addStageEvent(
                ArgumentMatchers.anyString(),
                eq(MessageAuditStage.API_CALL_FAILED),
                eq(MessageAuditEventStatus.RETRY),
                ArgumentMatchers.anyMap(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("После исчерпания 3 попыток возвращается BotException")
    void sendToUser_retriesExhausted_throwsBotException() {
        when(restTemplate.exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway #1"))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway #2"))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway #3"));

        assertThatThrownBy(() -> service.sendPlainTextToUser(USER_ID, MESSAGE_TEXT))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("StickerBot API ошибка");

        verify(restTemplate, times(3)).exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)
        );
        verify(messageAuditService, times(1)).finishFailure(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyMap()
        );
    }

    @Test
    @DisplayName("403 не ретраится и завершается после первой попытки")
    void sendToUser_forbidden_noRetry() {
        when(restTemplate.exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        new HttpHeaders(),
                        "{\"detail\":\"blocked\"}".getBytes(),
                        null
                ));

        assertThatThrownBy(() -> service.sendPlainTextToUser(USER_ID, MESSAGE_TEXT))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("403");

        verify(restTemplate, times(1)).exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)
        );
    }

    @Configuration
    @EnableRetry
    static class TestConfig {
        @Bean
        RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        MessageAuditService messageAuditService() {
            return mock(MessageAuditService.class);
        }

        @Bean
        AppConfig appConfig() {
            AppConfig appConfig = new AppConfig();
            AppConfig.StickerBot stickerBot = new AppConfig.StickerBot();
            stickerBot.setApiUrl(API_URL);
            stickerBot.setServiceToken("test-bearer-token");
            AppConfig.Retry retry = new AppConfig.Retry();
            retry.setMaxAttempts(3);
            retry.setInitialDelayMs(1L);
            retry.setMultiplier(1.0d);
            stickerBot.setRetry(retry);
            appConfig.setStickerbot(stickerBot);
            return appConfig;
        }

        @Bean
        StickerBotMessageService stickerBotMessageService(
                RestTemplate restTemplate,
                AppConfig appConfig,
                MessageAuditService messageAuditService) {
            return new StickerBotMessageService(restTemplate, appConfig, messageAuditService);
        }
    }
}

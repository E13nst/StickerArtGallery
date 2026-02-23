package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageResponse;
import com.example.sticker_art_gallery.exception.BotException;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Epic("Интеграция с StickerBot API")
@Feature("Отправка сообщений пользователю")
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты StickerBotMessageService")
class StickerBotMessageServiceTest {

    private static final String API_URL = "https://stickerbot.example.com";
    private static final String SERVICE_TOKEN = "test-bearer-token";
    private static final Long USER_ID = 123456789L;
    private static final String MESSAGE_TEXT = "Hello from test";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AppConfig appConfig;

    private AppConfig.StickerBot stickerBotConfig;
    private StickerBotMessageService service;

    @BeforeEach
    void setUp() {
        stickerBotConfig = new AppConfig.StickerBot();
        stickerBotConfig.setApiUrl(API_URL);
        stickerBotConfig.setServiceToken(SERVICE_TOKEN);
        when(appConfig.getStickerbot()).thenReturn(stickerBotConfig);
        service = new StickerBotMessageService(restTemplate, appConfig);
    }

    @Test
    @Story("Успешная отправка")
    @DisplayName("sendToUser при успешном ответе возвращает SendBotMessageResponse")
    void sendToUser_success_returnsResponse() {
        SendBotMessageRequest request = SendBotMessageRequest.builder()
                .userId(USER_ID)
                .text(MESSAGE_TEXT)
                .parseMode("plain")
                .build();
        SendBotMessageResponse expectedResponse = new SendBotMessageResponse("sent", 123456789, 42, "plain");
        when(restTemplate.exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        SendBotMessageResponse result = service.sendToUser(request);

        assertThat(result).isNotNull();
        assertThat(result.isSent()).isTrue();
        assertThat(result.getChatId()).isEqualTo(123456789);
        assertThat(result.getMessageId()).isEqualTo(42);

        ArgumentCaptor<HttpEntity<SendBotMessageRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(SendBotMessageResponse.class)
        );
        assertThat(captor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + SERVICE_TOKEN);
        assertThat(captor.getValue().getBody()).isSameAs(request);
    }

    @Test
    @Story("Успешная отправка")
    @DisplayName("sendPlainTextToUser формирует запрос с parse_mode plain")
    void sendPlainTextToUser_success_returnsResponse() {
        SendBotMessageResponse expectedResponse = new SendBotMessageResponse("sent", 999, 1, "plain");
        when(restTemplate.exchange(
                eq(API_URL + "/api/messages/send"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(SendBotMessageResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        SendBotMessageResponse result = service.sendPlainTextToUser(USER_ID, MESSAGE_TEXT);

        assertThat(result).isNotNull();
        assertThat(result.isSent()).isTrue();
        ArgumentCaptor<HttpEntity<SendBotMessageRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(API_URL + "/api/messages/send"), eq(HttpMethod.POST), captor.capture(), eq(SendBotMessageResponse.class));
        SendBotMessageRequest body = captor.getValue().getBody();
        assertThat(body.getUserId()).isEqualTo(USER_ID);
        assertThat(body.getText()).isEqualTo(MESSAGE_TEXT);
        assertThat(body.getParseMode()).isEqualTo("plain");
    }

    @Test
    @Story("Ошибка конфигурации")
    @DisplayName("sendToUser при пустом api-url выбрасывает BotException")
    void sendToUser_emptyApiUrl_throwsBotException() {
        stickerBotConfig.setApiUrl("");
        service = new StickerBotMessageService(restTemplate, appConfig);
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("API URL не настроен");
    }

    @Test
    @Story("Ошибка конфигурации")
    @DisplayName("sendToUser при пустом service-token выбрасывает BotException")
    void sendToUser_emptyToken_throwsBotException() {
        stickerBotConfig.setServiceToken("");
        service = new StickerBotMessageService(restTemplate, appConfig);
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("service token не настроен");
    }

    @Test
    @Story("Ошибка внешнего API")
    @DisplayName("sendToUser при 401 от API выбрасывает BotException")
    void sendToUser_api401_throwsBotException() {
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();
        when(restTemplate.exchange(eq(API_URL + "/api/messages/send"), eq(HttpMethod.POST), any(HttpEntity.class), eq(SendBotMessageResponse.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("StickerBot API ошибка")
                .hasMessageContaining("401");
    }

    @Test
    @Story("Ошибка внешнего API")
    @DisplayName("sendToUser при 502 от API выбрасывает BotException")
    void sendToUser_api502_throwsBotException() {
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();
        when(restTemplate.exchange(eq(API_URL + "/api/messages/send"), eq(HttpMethod.POST), any(HttpEntity.class), eq(SendBotMessageResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway"));

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("StickerBot API ошибка");
    }

    @Test
    @Story("Сетевая ошибка")
    @DisplayName("sendToUser при RestClientException выбрасывает BotException")
    void sendToUser_networkError_throwsBotException() {
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();
        when(restTemplate.exchange(eq(API_URL + "/api/messages/send"), eq(HttpMethod.POST), any(HttpEntity.class), eq(SendBotMessageResponse.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("Ошибка при отправке сообщения");
    }

    @Test
    @Story("Ответ API")
    @DisplayName("sendToUser при статусе отличном от sent выбрасывает BotException")
    void sendToUser_statusNotSent_throwsBotException() {
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();
        SendBotMessageResponse badResponse = new SendBotMessageResponse("error", null, null, null);
        when(restTemplate.exchange(eq(API_URL + "/api/messages/send"), eq(HttpMethod.POST), any(HttpEntity.class), eq(SendBotMessageResponse.class)))
                .thenReturn(ResponseEntity.ok(badResponse));

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("Отправка сообщения не удалась");
    }

    @Test
    @Story("Ответ API")
    @DisplayName("sendToUser при пустом теле ответа выбрасывает BotException")
    void sendToUser_nullBody_throwsBotException() {
        SendBotMessageRequest request = SendBotMessageRequest.builder().userId(USER_ID).text(MESSAGE_TEXT).build();
        when(restTemplate.exchange(eq(API_URL + "/api/messages/send"), eq(HttpMethod.POST), any(HttpEntity.class), eq(SendBotMessageResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> service.sendToUser(request))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("Пустой ответ");
    }
}

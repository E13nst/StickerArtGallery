package com.example.sticker_art_gallery.service.telegram.chat;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StickerGalleryFlowService")
class StickerGalleryFlowServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.Telegram telegramConfig;

    @Mock
    private StickerSetRepository stickerSetRepository;

    @Mock
    private StickerSetService stickerSetService;

    @Mock
    private TelegramBotApiService telegramBotApiService;

    private StickerGalleryFlowService service;

    @BeforeEach
    void setUp() {
        when(appConfig.getTelegram()).thenReturn(telegramConfig);
        service = new StickerGalleryFlowService(appConfig, stickerSetRepository, stickerSetService, telegramBotApiService);
    }

    @Test
    @DisplayName("/start показывает экран подписки, если канал обязателен")
    void handleStart_ShouldShowSubscriptionGate_WhenSubscriptionRequired() throws Exception {
        when(telegramConfig.isChannelSubscriptionRequired()).thenReturn(true);
        when(telegramConfig.getRequiredChannelUrl()).thenReturn("https://t.me/stixlyofficial");
        when(telegramBotApiService.getRequiredChannelMembershipStatus(123L))
                .thenReturn(TelegramBotApiService.ChannelMembershipStatus.NOT_SUBSCRIBED);

        JsonNode message = objectMapper.readTree("""
                {
                  "chat": { "id": 555 },
                  "from": { "id": 123, "first_name": "Alex" }
                }
                """);

        service.handleStart(message);

        verify(telegramBotApiService).sendMessage(
                eq(555L),
                contains("сначала подпишитесь"),
                eq("HTML"),
                any(),
                isNull(),
                isNull()
        );
        verify(telegramBotApiService, never()).sendMessage(
                eq(555L),
                contains("Йо, Alex!"),
                eq("HTML"),
                any(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("/start открывает главное меню, если подписка подтверждена")
    void handleStart_ShouldShowMainMenu_WhenSubscriptionConfirmed() throws Exception {
        when(telegramConfig.isChannelSubscriptionRequired()).thenReturn(true);
        when(telegramBotApiService.getRequiredChannelMembershipStatus(123L))
                .thenReturn(TelegramBotApiService.ChannelMembershipStatus.SUBSCRIBED);

        JsonNode message = objectMapper.readTree("""
                {
                  "chat": { "id": 555 },
                  "from": { "id": 123, "first_name": "Alex" }
                }
                """);

        service.handleStart(message);

        verify(telegramBotApiService).sendMessage(
                eq(555L),
                contains("Йо, Alex!"),
                eq("HTML"),
                any(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("callback проверки подписки открывает меню после успешной проверки")
    void handleCallback_ShouldOpenMainMenu_WhenSubscriptionConfirmed() throws Exception {
        when(telegramConfig.isChannelSubscriptionRequired()).thenReturn(true);
        when(telegramBotApiService.getRequiredChannelMembershipStatus(123L))
                .thenReturn(TelegramBotApiService.ChannelMembershipStatus.SUBSCRIBED);

        JsonNode callbackQuery = objectMapper.readTree("""
                {
                  "id": "cb-1",
                  "data": "check_channel_subscription",
                  "from": { "id": 123, "first_name": "Alex" },
                  "message": {
                    "chat": { "id": 555 },
                    "message_id": 42
                  }
                }
                """);

        service.handleCallback(callbackQuery);

        verify(telegramBotApiService).editMessageText(
                eq(555L),
                eq(42L),
                contains("Йо, Alex!"),
                eq("HTML"),
                any()
        );
        verify(telegramBotApiService).answerCallbackQuery("cb-1", "Подписка подтверждена", false);
    }
}

package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageResponse;
import com.example.sticker_art_gallery.exception.BotException;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Нативный клиент для отправки сообщений пользователям через Telegram Bot API.
 *
 * Заменяет StickerBotMessageService при TELEGRAM_NATIVE_MESSAGING_ENABLED=true:
 * вместо проксирования через внешний StickerBot вызывает Telegram Bot API напрямую,
 * используя токен бота из app.telegram.bot-token.
 *
 * Интерфейс вызова идентичен StickerBotMessageService.
 */
@Service
public class TelegramMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramMessageService.class);

    private final TelegramBotApiService telegramBotApiService;

    public TelegramMessageService(TelegramBotApiService telegramBotApiService) {
        this.telegramBotApiService = telegramBotApiService;
    }

    /**
     * Отправить сообщение пользователю напрямую через Telegram Bot API.
     *
     * @param request запрос с userId, text, parseMode
     * @return ответ с messageId и chatId
     * @throws BotException при ошибке отправки
     */
    public SendBotMessageResponse sendToUser(SendBotMessageRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new BotException("userId обязателен для отправки сообщения");
        }
        if (request.getText() == null || request.getText().isBlank()) {
            throw new BotException("Текст сообщения не может быть пустым");
        }

        Long chatId = request.getUserId();
        String text = request.getText();
        String parseMode = request.getParseMode();

        LOGGER.debug("📤 TelegramMessageService.sendToUser: userId={}, textLen={}, parseMode={}",
                chatId, text.length(), parseMode);

        try {
            int messageId = telegramBotApiService.sendMessage(chatId, text, parseMode);
            if (messageId < 0) {
                throw new BotException("Telegram вернул некорректный messageId=" + messageId);
            }
            LOGGER.info("✅ Сообщение отправлено напрямую через Telegram API: userId={}, messageId={}", chatId, messageId);
            return SendBotMessageResponse.sent(chatId, messageId);
        } catch (BotException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка отправки сообщения через Telegram API: userId={}, error={}", chatId, e.getMessage());
            throw new BotException("Ошибка отправки сообщения через Telegram API: " + e.getMessage(), e);
        }
    }

    /**
     * Удобный метод: отправить текстовое plain-сообщение пользователю.
     */
    public SendBotMessageResponse sendPlainTextToUser(Long userId, String text) {
        return sendToUser(SendBotMessageRequest.builder()
                .userId(userId)
                .text(text)
                .parseMode("plain")
                .build());
    }
}

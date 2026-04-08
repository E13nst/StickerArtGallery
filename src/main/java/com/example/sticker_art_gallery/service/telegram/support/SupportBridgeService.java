package com.example.sticker_art_gallery.service.telegram.support;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SupportBridgeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportBridgeService.class);

    private static final Map<String, String> SUPPORT_TOPICS = Map.of(
            "author_claim", "🎨 Подтвердить авторство на стикерпак",
            "bug_report", "🐞 Нашёл баг",
            "improvement", "🔝 Предложение по улучшению",
            "other", "❔ Другое"
    );

    private final TelegramBotApiService telegramBotApiService;
    private final SupportStateStore supportStateStore;
    private final AppConfig appConfig;

    public SupportBridgeService(TelegramBotApiService telegramBotApiService,
                                SupportStateStore supportStateStore,
                                AppConfig appConfig) {
        this.telegramBotApiService = telegramBotApiService;
        this.supportStateStore = supportStateStore;
        this.appConfig = appConfig;
    }

    public boolean isEnabled() {
        return appConfig.getTelegram().isSupportEnabled() && appConfig.getTelegram().getSupportChatId() != null;
    }

    public void handleSupportCommand(long chatId, long userId) {
        if (!isEnabled()) {
            telegramBotApiService.sendMessage(chatId, "📞 Функция поддержки временно недоступна.", null, null, null, null);
            return;
        }
        telegramBotApiService.sendMessage(
                chatId,
                "📞 Режим поддержки\n\nВыберите тему обращения:",
                null,
                supportTopicsKeyboard(),
                null,
                null
        );
        supportStateStore.clearUserTopic(userId);
    }

    public boolean handleSupportCallback(JsonNode callbackQuery) {
        String data = callbackQuery.path("data").asText("");
        if (!data.startsWith("support_topic:") && !"exit_support".equals(data) && !"enter_support".equals(data)) {
            return false;
        }
        String callbackId = callbackQuery.path("id").asText();
        long chatId = callbackQuery.path("message").path("chat").path("id").asLong();
        long messageId = callbackQuery.path("message").path("message_id").asLong();
        long userId = callbackQuery.path("from").path("id").asLong();

        try {
            if ("enter_support".equals(data)) {
                telegramBotApiService.answerCallbackQuery(callbackId, null, false);
                handleSupportCommand(chatId, userId);
                return true;
            }
            if ("exit_support".equals(data)) {
                supportStateStore.clearUserTopic(userId);
                telegramBotApiService.editMessageText(
                        chatId,
                        messageId,
                        "Вы вышли из режима поддержки. Напишите /start для возврата в меню.",
                        null,
                        null
                );
                telegramBotApiService.answerCallbackQuery(callbackId, null, false);
                return true;
            }

            String topic = data.substring("support_topic:".length());
            if (!SUPPORT_TOPICS.containsKey(topic)) {
                telegramBotApiService.answerCallbackQuery(callbackId, "Неверная тема", true);
                return true;
            }
            supportStateStore.setUserTopic(userId, topic);
            telegramBotApiService.editMessageText(
                    chatId,
                    messageId,
                    "Тема выбрана: " + SUPPORT_TOPICS.get(topic) + "\n\n"
                            + "Теперь отправьте сообщение (текст/фото/документ/голос/видео/стикер).",
                    null,
                    supportExitKeyboard()
            );
            telegramBotApiService.answerCallbackQuery(callbackId, "Тема выбрана", false);
            return true;
        } catch (Exception e) {
            LOGGER.error("Support callback handling failed: {}", e.getMessage(), e);
            telegramBotApiService.answerCallbackQuery(callbackId, "Ошибка, попробуйте снова", true);
            return true;
        }
    }

    public boolean handleUserMessage(JsonNode message) {
        if (!isEnabled()) {
            return false;
        }
        long userId = message.path("from").path("id").asLong(0);
        long chatId = message.path("chat").path("id").asLong(0);
        String topic = supportStateStore.getUserTopic(userId);
        if (topic == null) {
            return false;
        }

        Long supportChatId = appConfig.getTelegram().getSupportChatId();
        if (supportChatId == null) {
            return false;
        }

        try {
            Integer threadId = null;
            if (appConfig.getTelegram().isSupportUseTopics()) {
                threadId = getOrCreateThreadForUser(userId, topic);
            }

            String userName = message.path("from").path("first_name").asText("Unknown");
            String username = message.path("from").path("username").asText("");
            String infoText = "👤 Пользователь: " + userName + "\n"
                    + "🆔 ID: " + userId + "\n"
                    + "📛 Username: " + (username.isBlank() ? "нет" : "@" + username) + "\n"
                    + "📂 Тема: " + SUPPORT_TOPICS.getOrDefault(topic, topic) + "\n\n"
                    + "📩 Сообщение:";

            telegramBotApiService.sendMessage(supportChatId, infoText, null, null, null, threadId);

            Long forwardedMessageId = forwardUserContent(message, supportChatId, threadId);
            if (forwardedMessageId != null) {
                supportStateStore.saveSupportMessageMapping(forwardedMessageId, userId);
            }

            telegramBotApiService.sendMessage(chatId,
                    "✅ Ваше сообщение передано в поддержку. Ответим в ближайшее время.",
                    null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.error("Support forward failed: {}", e.getMessage(), e);
            telegramBotApiService.sendMessage(chatId,
                    "⚠️ Не удалось отправить сообщение в поддержку. Попробуйте позже.",
                    null, null, null, null);
            return true;
        }
    }

    public boolean handleSupportReply(JsonNode message) {
        if (!isEnabled()) {
            return false;
        }
        long chatId = message.path("chat").path("id").asLong(0);
        Long supportChatId = appConfig.getTelegram().getSupportChatId();
        if (supportChatId == null || chatId != supportChatId) {
            return false;
        }
        if (!message.has("reply_to_message")) {
            return false;
        }

        long repliedMessageId = message.path("reply_to_message").path("message_id").asLong(0);
        Long userId = supportStateStore.findUserBySupportMessageId(repliedMessageId);
        if (userId == null) {
            return false;
        }

        try {
            sendReplyToUser(message, userId);
            telegramBotApiService.sendMessage(supportChatId, "✅ Ответ отправлен пользователю.", null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to relay support reply to user {}: {}", userId, e.getMessage(), e);
            telegramBotApiService.sendMessage(supportChatId, "❌ Ошибка отправки ответа пользователю.", null, null, null, null);
            return true;
        }
    }

    private Integer getOrCreateThreadForUser(long userId, String topic) {
        Integer existing = supportStateStore.getThreadForUserTopic(userId, topic);
        if (existing != null) {
            return existing;
        }
        Long supportChatId = appConfig.getTelegram().getSupportChatId();
        if (supportChatId == null) {
            return null;
        }
        String topicName = SUPPORT_TOPICS.getOrDefault(topic, "❔ Другое");
        Integer created = telegramBotApiService.createForumTopic(supportChatId, topicName + " " + userId);
        if (created != null) {
            supportStateStore.saveThreadForUserTopic(userId, topic, created);
        }
        return created;
    }

    private Long forwardUserContent(JsonNode message, Long supportChatId, Integer threadId) {
        if (message.hasNonNull("text")) {
            return (long) telegramBotApiService.sendMessage(supportChatId, message.path("text").asText(), null, null, null, threadId);
        }
        if (message.has("photo") && message.path("photo").isArray() && !message.path("photo").isEmpty()) {
            JsonNode largest = message.path("photo").get(message.path("photo").size() - 1);
            String fileId = largest.path("file_id").asText();
            return (long) telegramBotApiService.sendPhoto(
                    supportChatId,
                    fileId,
                    message.path("caption").asText(null),
                    "Markdown",
                    null,
                    threadId
            );
        }
        if (message.has("document")) {
            return (long) telegramBotApiService.sendDocument(
                    supportChatId,
                    message.path("document").path("file_id").asText(),
                    message.path("caption").asText(null),
                    "Markdown",
                    null,
                    threadId
            );
        }
        if (message.has("voice")) {
            return (long) telegramBotApiService.sendVoice(
                    supportChatId,
                    message.path("voice").path("file_id").asText(),
                    message.path("caption").asText(null),
                    "Markdown",
                    null,
                    threadId
            );
        }
        if (message.has("video")) {
            return (long) telegramBotApiService.sendVideo(
                    supportChatId,
                    message.path("video").path("file_id").asText(),
                    message.path("caption").asText(null),
                    "Markdown",
                    null,
                    threadId
            );
        }
        if (message.has("sticker")) {
            return (long) telegramBotApiService.sendSticker(
                    supportChatId,
                    message.path("sticker").path("file_id").asText(),
                    null,
                    threadId
            );
        }
        return null;
    }

    private void sendReplyToUser(JsonNode message, long userId) {
        Object supportButton = Map.of("inline_keyboard", List.of(
                List.of(Map.of("text", "📞 Связаться с поддержкой", "callback_data", "enter_support"))
        ));

        if (message.hasNonNull("text")) {
            telegramBotApiService.sendMessage(
                    userId,
                    "👨‍💼 Поддержка:\n\n" + message.path("text").asText(),
                    "Markdown",
                    supportButton,
                    null,
                    null
            );
            return;
        }
        if (message.has("photo") && message.path("photo").isArray() && !message.path("photo").isEmpty()) {
            JsonNode largest = message.path("photo").get(message.path("photo").size() - 1);
            telegramBotApiService.sendPhoto(
                    userId,
                    largest.path("file_id").asText(),
                    withSupportPrefix(message.path("caption").asText(null)),
                    "Markdown",
                    supportButton,
                    null
            );
            return;
        }
        if (message.has("document")) {
            telegramBotApiService.sendDocument(
                    userId,
                    message.path("document").path("file_id").asText(),
                    withSupportPrefix(message.path("caption").asText(null)),
                    "Markdown",
                    supportButton,
                    null
            );
            return;
        }
        if (message.has("voice")) {
            telegramBotApiService.sendVoice(
                    userId,
                    message.path("voice").path("file_id").asText(),
                    withSupportPrefix(message.path("caption").asText(null)),
                    "Markdown",
                    supportButton,
                    null
            );
            return;
        }
        if (message.has("video")) {
            telegramBotApiService.sendVideo(
                    userId,
                    message.path("video").path("file_id").asText(),
                    withSupportPrefix(message.path("caption").asText(null)),
                    "Markdown",
                    supportButton,
                    null
            );
            return;
        }
        if (message.has("sticker")) {
            telegramBotApiService.sendSticker(
                    userId,
                    message.path("sticker").path("file_id").asText(),
                    supportButton,
                    null
            );
            telegramBotApiService.sendMessage(userId, "👨‍💼 Поддержка", "Markdown", null, null, null);
        }
    }

    private String withSupportPrefix(String caption) {
        if (caption == null || caption.isBlank()) {
            return "👨‍💼 Поддержка";
        }
        return "👨‍💼 Поддержка:\n\n" + caption;
    }

    private Object supportTopicsKeyboard() {
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", SUPPORT_TOPICS.get("author_claim"), "callback_data", "support_topic:author_claim")),
                        List.of(Map.of("text", SUPPORT_TOPICS.get("bug_report"), "callback_data", "support_topic:bug_report")),
                        List.of(Map.of("text", SUPPORT_TOPICS.get("improvement"), "callback_data", "support_topic:improvement")),
                        List.of(Map.of("text", SUPPORT_TOPICS.get("other"), "callback_data", "support_topic:other")),
                        List.of(Map.of("text", "◀️ Назад в меню", "callback_data", "exit_support"))
                )
        );
    }

    private Object supportExitKeyboard() {
        return Map.of(
                "inline_keyboard",
                List.of(List.of(Map.of("text", "◀️ Назад в меню", "callback_data", "exit_support")))
        );
    }
}

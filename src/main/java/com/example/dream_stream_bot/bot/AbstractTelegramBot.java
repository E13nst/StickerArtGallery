package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;

public abstract class AbstractTelegramBot extends TelegramLongPollingBot {
    protected final BotEntity botEntity;
    protected MessageHandlerService messageHandlerService;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTelegramBot.class);

    public AbstractTelegramBot(BotEntity botEntity, MessageHandlerService messageHandlerService) {
        this.botEntity = botEntity;
        this.messageHandlerService = messageHandlerService;
    }

    @Override
    public String getBotUsername() {
        return botEntity.getUsername();
    }

    @Override
    public String getBotToken() {
        return botEntity.getToken();
    }

    @Override
    public abstract void onUpdateReceived(Update update);

    protected String getConversationId(Long chatId) {
        return getBotUsername() + ":" + chatId;
    }

    protected void sendTypingActionWithDuration(Long chatId, String text) {
        int charsPerSecond = 20; // скорость "печати"
        int minSeconds = 1;
        int maxSeconds = 5;
        int duration = Math.max(minSeconds, Math.min(maxSeconds, text != null ? text.length() / charsPerSecond : minSeconds));
        SendChatAction chatAction = new SendChatAction();
        chatAction.setChatId(chatId.toString());
        chatAction.setAction(ActionType.TYPING);
        try {
            for (int i = 0; i < duration; i++) {
                execute(chatAction);
                Thread.sleep(1000);
            }
            LOGGER.info("✍️ Sent typing action to chat: {} for {} seconds", chatId, duration);
            Thread.sleep(500); // небольшая задержка перед отправкой сообщения
        } catch (TelegramApiException | InterruptedException e) {
            LOGGER.error("❌ Failed to send typing action | Chat: {} | Error: {}", chatId, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    protected void sendWithLogging(SendMessage message) {
        sendTypingActionWithDuration(Long.valueOf(message.getChatId()), message.getText());
        try {
            execute(message);
            LOGGER.info("✅ Message sent | Chat: {} | Text: '{}'", message.getChatId(), truncateText(message.getText(), 100));
        } catch (TelegramApiException e) {
            LOGGER.error("❌ Failed to send message | Chat: {} | Error: {}", message.getChatId(), e.getMessage(), e);
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // Пример для будущих ботов:
    // protected void handleReplyToBotMessage(Message message, String conversationId) {
    //     messageHandlerService.handleReplyToBotMessage(message, conversationId, botEntity);
    // }
    // protected void handlePersonalMessage(Message message, String conversationId) {
    //     messageHandlerService.handlePersonalMessage(message, conversationId, botEntity);
    // }

    // Можно добавить общие методы для всех ботов, например, логирование, отправку сообщений и т.д.
} 
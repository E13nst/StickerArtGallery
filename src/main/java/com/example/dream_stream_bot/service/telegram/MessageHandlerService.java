package com.example.dream_stream_bot.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);

    @Autowired
    private com.example.dream_stream_bot.service.ai.AIService aiService;

    // Обработчик ответов на сообщения бота в чате
    public List<SendMessage> handleReplyToBotMessage(Message message, String conversationId, com.example.dream_stream_bot.model.telegram.BotEntity botEntity) {
        User user = message.getFrom();
        LOGGER.info("💭 Handling reply to bot message | User: {} (@{}) | Text: '{}' | ChatId: {} | ConversationId: {}", 
            user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50), message.getChatId(), conversationId);
        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(message.getChatId());
        String groupMessage = "User " + chatUserName(user) + " says:\n" + message.getText();
        String response = aiService.completion(conversationId, groupMessage, botEntity.getPrompt(), botEntity.getMemWindow());
        LOGGER.info("💬 Bot response to chatId {}: '{}'", message.getChatId(), truncateText(response, 100));
        sendMessages.add(msgFactory.createReplyToMessage(response, message.getMessageId()));
        LOGGER.info("💭 Reply message prepared | User: {} (@{}) | Response length: {} chars | ChatId: {}", 
            user.getFirstName(), user.getUserName(), response.length(), message.getChatId());
        return sendMessages;
    }

    public List<SendMessage> handlePersonalMessage(Message message, String conversationId, com.example.dream_stream_bot.model.telegram.BotEntity botEntity) {
        User user = message.getFrom();
        LOGGER.info("💭 Handling personal message | User: {} (@{}) | Text: '{}' | ChatId: {} | ConversationId: {}", 
            user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50), message.getChatId(), conversationId);
        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(message.getChatId());
        String prompt = botEntity.getPrompt() != null ? botEntity.getPrompt() : "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            prompt = prompt + "\nUser name is " + user.getFirstName() + ".";
        }
        String response = aiService.completion(conversationId, message.getText(), prompt, botEntity.getMemWindow());
        LOGGER.info("💬 Bot response to chatId {}: '{}'", message.getChatId(), truncateText(response, 100));
        sendMessages.add(msgFactory.createMarkdownMessage(response));
        return sendMessages;
    }

    public static String chatUserName(User user) {
        if (user.getFirstName() != null) {
            return user.getFirstName();
        } else if (user.getUserName() != null) {
            return user.getUserName();
        } else {
            return "User" + user.getId();
        }
    }

    public static String transliterateUserName(User user) {
        return user.getFirstName();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

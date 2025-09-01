package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class CopyCatBot extends AbstractTelegramBot {
    public CopyCatBot(BotEntity botEntity, MessageHandlerService messageHandlerService) {
        super(botEntity, messageHandlerService);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            String text = msg.getText();
            String conversationId = getConversationId(msg.getChatId());
            // Если потребуется память — использовать conversationId
            SendMessage reply = SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text(text)
                    .build();
            sendWithLogging(reply);
        }
    }
} 
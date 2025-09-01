package com.example.dream_stream_bot.service.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class TelegramMessageFactory {

    private final long chatId;

    public TelegramMessageFactory(long chatId) {
        this.chatId = chatId;
    }

    public SendMessage createMarkdownMessage(String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
    }

    public SendMessage createMarkdownMessage(String text, InlineKeyboardMarkup keyboard) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    public SendMessage createReplyToMessage(String text, int replyToMessageId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyToMessageId(replyToMessageId)
                .build();
    }
}

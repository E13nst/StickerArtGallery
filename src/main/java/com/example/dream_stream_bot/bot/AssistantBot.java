package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AssistantBot extends AbstractTelegramBot {
    public AssistantBot(BotEntity botEntity, MessageHandlerService messageHandlerService) {
        super(botEntity, messageHandlerService);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            String conversationId = getConversationId(msg.getChatId());
            boolean isGroup = msg.isGroupMessage() || msg.isSuperGroupMessage();
            boolean isReplyToBot = msg.getReplyToMessage() != null &&
                    msg.getReplyToMessage().getFrom() != null &&
                    msg.getReplyToMessage().getFrom().getUserName() != null &&
                    msg.getReplyToMessage().getFrom().getUserName().equalsIgnoreCase(getBotUsername());
            boolean isMention = msg.getText().toLowerCase().contains("@" + getBotUsername().toLowerCase());
            boolean isName = msg.getText().toLowerCase().contains(botEntity.getName().toLowerCase());
            boolean isAlias = botEntity.getBotAliasesList().stream().anyMatch(alias -> !alias.isEmpty() && msg.getText().toLowerCase().contains(alias.toLowerCase()));
            boolean isTrigger = botEntity.getBotTriggersList().stream().anyMatch(trigger -> !trigger.isEmpty() && msg.getText().toLowerCase().contains(trigger.toLowerCase()));
            if (isGroup) {
                if (!(isReplyToBot || isMention || isName || isAlias || isTrigger)) {
                    // Игнорируем сообщение, если не обращение к боту
                    return;
                }
                // В группе всегда отвечаем с reply
                var responses = messageHandlerService.handleReplyToBotMessage(msg, conversationId, botEntity);
                for (var response : responses) {
                    sendWithLogging(response);
                }
            } else {
                // Личное сообщение — обычная обработка
                var responses = messageHandlerService.handlePersonalMessage(msg, conversationId, botEntity);
                for (var response : responses) {
                    sendWithLogging(response);
                }
            }
        }
    }
} 
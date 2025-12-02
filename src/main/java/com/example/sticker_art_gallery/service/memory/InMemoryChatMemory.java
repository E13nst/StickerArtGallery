package com.example.sticker_art_gallery.service.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryChatMemory implements ChatMemory {

    // Потокобезопасная карта для хранения сообщений по conversationId
    private final ConcurrentMap<String, List<Message>> conversations = new ConcurrentHashMap<>();
    
    // Настраиваемая длина контекста (по умолчанию 10 сообщений)
    @Value("${app.ai.context-length:10}")
    private int contextLength;

    // Добавляет список сообщений к указанному разговору
    // Ограничивает размер истории сообщений до contextLength
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversation = conversations.computeIfAbsent(conversationId, k -> new ArrayList<>());
        conversation.addAll(messages);
        
        // Ограничиваем размер истории до contextLength (оставляем только последние N сообщений)
        if (conversation.size() > contextLength) {
            int startIndex = conversation.size() - contextLength;
            List<Message> limitedConversation = new ArrayList<>(conversation.subList(startIndex, conversation.size()));
            conversations.put(conversationId, limitedConversation);
        }
    }

    // Возвращает все сообщения из указанного разговора
    // В Spring AI 1.0.0 метод get() принимает только conversationId
    @Override
    public List<Message> get(String conversationId) {
        List<Message> conversation = conversations.getOrDefault(conversationId, Collections.emptyList());
        // Ограничиваем размер истории до contextLength при возврате
        int size = conversation.size();
        if (size <= contextLength) {
            return new ArrayList<>(conversation);
        } else {
            return new ArrayList<>(conversation.subList(size - contextLength, size));
        }
    }

    // Очищает историю сообщений для указанного разговора
    @Override
    public void clear(String conversationId) {
        conversations.remove(conversationId);
    }
}

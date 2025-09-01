package com.example.dream_stream_bot.service.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

public class InMemoryChatMemory implements ChatMemory {

    // Потокобезопасная карта для хранения сообщений по conversationId
    private final ConcurrentMap<String, List<Message>> conversations = new ConcurrentHashMap<>();

    // Добавляет список сообщений к указанному разговору
    @Override
    public void add(String conversationId, List<Message> messages) {
        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
    }

    // Возвращает последние N сообщений из указанного разговора
    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> conversation = conversations.getOrDefault(conversationId, Collections.emptyList());
        int size = conversation.size();
        if (size <= lastN) {
            return new ArrayList<>(conversation);
        } else {
            return new ArrayList<>(conversation.subList(size - lastN, size));
        }
    }

    // Очищает историю сообщений для указанного разговора
    @Override
    public void clear(String conversationId) {
        conversations.remove(conversationId);
    }
}

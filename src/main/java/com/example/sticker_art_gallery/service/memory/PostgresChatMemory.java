package com.example.sticker_art_gallery.service.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostgresChatMemory implements ChatMemory {
    @Autowired
    private ChatMemoryRepository repository;

    @Override
    public void add(String conversationId, List<Message> messages) {
        int startIndex = repository.countByConversationId(conversationId);
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            ChatMemoryEntity entity = new ChatMemoryEntity();
            entity.setConversationId(conversationId);
            entity.setMessageIndex(startIndex + i);
            entity.setRole(msg.getMessageType().name().toLowerCase());
            entity.setContent(msg.getContent());
            repository.save(entity);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMemoryEntity> entities = repository.findByConversationIdOrderByMessageIndexAsc(conversationId);
        return entities.stream()
            .skip(Math.max(0, entities.size() - lastN))
            .map(e -> {
                String role = e.getRole().toLowerCase();
                switch (role) {
                    case "user": return new UserMessage(e.getContent());
                    case "assistant": return new AssistantMessage(e.getContent());
                    case "system": return new SystemMessage(e.getContent());
                    default: throw new IllegalArgumentException("Unknown role: " + e.getRole());
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }
} 
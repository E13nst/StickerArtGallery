package com.example.sticker_art_gallery.service.memory;

import com.example.sticker_art_gallery.repository.ChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// @Component - отключено, используем InMemoryChatMemory
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
            // В Spring AI 1.0.0 Message имеет метод getContent() который возвращает Content
            // Content может содержать текст и/или медиа
            String content = "";
            try {
                // Пробуем получить текст через getContent()
                Object messageContent = msg.getClass().getMethod("getContent").invoke(msg);
                if (messageContent != null) {
                    // Content может быть списком или объектом с методом getText()
                    if (messageContent instanceof List) {
                        List<?> contentList = (List<?>) messageContent;
                        content = contentList.stream()
                            .map(c -> {
                                try {
                                    return (String) c.getClass().getMethod("getText").invoke(c);
                                } catch (Exception e) {
                                    return c.toString();
                                }
                            })
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(Collectors.joining(" "));
                    } else {
                        try {
                            content = (String) messageContent.getClass().getMethod("getText").invoke(messageContent);
                        } catch (Exception e) {
                            content = messageContent.toString();
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback: используем toString()
                content = msg.toString();
            }
            if (content == null || content.isEmpty()) {
                content = msg.toString();
            }
            entity.setContent(content);
            repository.save(entity);
        }
    }

    // В Spring AI 1.0.0 метод get() принимает только conversationId
    @Override
    public List<Message> get(String conversationId) {
        List<ChatMemoryEntity> entities = repository.findByConversationIdOrderByMessageIndexAsc(conversationId);
        return entities.stream()
            .map(e -> {
                String role = e.getRole().toLowerCase();
                String content = e.getContent();
                switch (role) {
                    case "user": return new UserMessage(content);
                    case "assistant": return new AssistantMessage(content);
                    case "system": return new SystemMessage(content);
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
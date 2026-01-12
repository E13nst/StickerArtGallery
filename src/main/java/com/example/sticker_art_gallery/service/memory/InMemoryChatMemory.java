package com.example.sticker_art_gallery.service.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * In-memory chat memory implementation with LRU eviction policy.
 * Limits both the number of conversations and messages per conversation to prevent memory leaks.
 */
@Component
public class InMemoryChatMemory implements ChatMemory {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryChatMemory.class);
    
    // Максимальное количество conversations в памяти
    private static final int MAX_CONVERSATIONS = 100;

    // Настраиваемая длина контекста (по умолчанию 10 сообщений)
    @Value("${app.ai.context-length:10}")
    private int contextLength;

    // Потокобезопасная карта с LRU eviction для хранения сообщений по conversationId
    // LinkedHashMap с accessOrder=true автоматически перемещает используемые элементы в конец
    // removeEldestEntry удаляет старые conversations при превышении лимита
    private final Map<String, List<Message>> conversations = Collections.synchronizedMap(
        new LinkedHashMap<String, List<Message>>(MAX_CONVERSATIONS, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<Message>> eldest) {
                boolean shouldRemove = size() > MAX_CONVERSATIONS;
                if (shouldRemove) {
                    LOGGER.debug("🗑️ Evicting oldest conversation: {} (total conversations: {})", eldest.getKey(), size());
                }
                return shouldRemove;
            }
        }
    );

    /**
     * Добавляет список сообщений к указанному разговору.
     * Ограничивает размер истории сообщений до contextLength.
     */
    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        synchronized (conversations) {
            List<Message> conversation = conversations.computeIfAbsent(conversationId, k -> new ArrayList<>());
            conversation.addAll(messages);
            
            // Ограничиваем размер истории до contextLength (оставляем только последние N сообщений)
            if (conversation.size() > contextLength) {
                int startIndex = conversation.size() - contextLength;
                List<Message> limitedConversation = new ArrayList<>(conversation.subList(startIndex, conversation.size()));
                conversations.put(conversationId, limitedConversation);
                LOGGER.trace("Trimmed conversation {} to {} messages", conversationId, contextLength);
            }
        }
    }

    /**
     * Возвращает все сообщения из указанного разговора.
     * Ограничивает возвращаемый результат до contextLength сообщений.
     */
    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        List<Message> conversation = conversations.getOrDefault(conversationId, Collections.emptyList());
        // Ограничиваем размер истории до contextLength при возврате
        int size = conversation.size();
        if (size <= contextLength) {
            return new ArrayList<>(conversation);
        } else {
            return new ArrayList<>(conversation.subList(size - contextLength, size));
        }
    }

    /**
     * Очищает историю сообщений для указанного разговора.
     */
    @Override
    public void clear(@NonNull String conversationId) {
        conversations.remove(conversationId);
        LOGGER.debug("Cleared conversation: {}", conversationId);
    }
    
    /**
     * Возвращает текущее количество conversations в памяти.
     * Полезно для мониторинга.
     */
    public int getConversationCount() {
        return conversations.size();
    }
}

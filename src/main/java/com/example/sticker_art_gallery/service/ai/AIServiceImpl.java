package com.example.sticker_art_gallery.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    @Value("${bot.memory-window-size:100}")
    private int memoryWindowSize;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Autowired
    public AIServiceImpl(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @Override
    public String completion(String conversationId, String message, String prompt, Integer memWindow) {
        logger.info("\uD83E\uDD16 AI Request | Conversation: {} | Message: '{}'", 
            conversationId, truncateText(message, 100));
        String response = chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, memWindow != null ? memWindow : 100))
                .system(prompt)
                .user(message)
                .call()
                .content();
        logger.info("\uD83E\uDD16 AI Response | Conversation: {} | Length: {} chars", 
            conversationId, response.length());
        logger.debug("\uD83E\uDD16 AI Response content | Conversation: {} | Text: '{}'", 
            conversationId, truncateText(response, 200));
        return response;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

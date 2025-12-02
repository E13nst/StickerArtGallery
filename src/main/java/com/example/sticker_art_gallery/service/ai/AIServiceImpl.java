package com.example.sticker_art_gallery.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

// В Spring AI 1.0.0 изменился API для advisors
// Используем прямой вызов без статических импортов

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
        logger.info("\uD83E\uDD16 AI Request | Conversation: {} | Message length: {} chars", 
            conversationId, message != null ? message.length() : 0);
        logger.info("\uD83E\uDD16 AI Request full message:\n{}", message);
        logger.info("\uD83E\uDD16 AI Request system prompt:\n{}", prompt);
        // В Spring AI 1.0.0 ChatMemory автоматически используется через ChatMemoryAdvisor
        String response = chatClient.prompt()
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

    @Override
    public String completionWithImage(String conversationId, String systemPrompt, String userPrompt, 
                                      byte[] imageData, String imageMimeType) {
        if (imageData == null) {
            throw new IllegalArgumentException("imageData cannot be null");
        }
        
        logger.info("🤖 AI Request with Image | Conversation: {} | Message length: {} chars | Image size: {} bytes", 
            conversationId, userPrompt != null ? userPrompt.length() : 0, imageData.length);
        logger.debug("🤖 AI Request system prompt:\n{}", systemPrompt);
        logger.debug("🤖 AI Request user prompt:\n{}", userPrompt);
        
        // Создаем Resource из изображения
        Resource imageResource = new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "sticker-set-image.webp";
            }
        };
        
        logger.info("📤 Preparing to send request to OpenAI:");
        logger.info("  - Image resource: {} ({} bytes)", imageResource.getFilename(), imageData.length);
        logger.info("  - Image MIME type: {}", imageMimeType);
        logger.info("  - System prompt length: {} chars", systemPrompt != null ? systemPrompt.length() : 0);
        logger.info("  - User prompt length: {} chars", userPrompt != null ? userPrompt.length() : 0);
        
        // В Spring AI 1.0.0 используем прямой вызов через ChatClient
        // Передаем текст и изображение через user() метод
        // Используем экранирование специальных символов в промпте, чтобы избежать интерпретации как шаблона
        String escapedPrompt = userPrompt.replace("${", "\\${").replace("#{", "\\#{");
        
        logger.info("⏱️ Starting OpenAI API call at: {}", java.time.LocalDateTime.now());
        long startTime = System.currentTimeMillis();
        
        try {
            // В Spring AI 1.0.0 ChatMemory автоматически используется через ChatMemoryAdvisor
            // Передаем текст и изображение через два вызова user()
            // Изображение передается как Resource, Spring AI должен автоматически обработать его как медиа
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(escapedPrompt)
                    .user(imageResource)
                    .call()
                    .content();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ OpenAI API call completed successfully in {} ms ({} seconds)", duration, duration / 1000.0);
            logger.info("🤖 AI Response | Conversation: {} | Length: {} chars", 
                conversationId, response.length());
            logger.debug("🤖 AI Response content | Conversation: {} | Text: '{}'", 
                conversationId, truncateText(response, 200));
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("❌ OpenAI API call failed after {} ms ({} seconds)", duration, duration / 1000.0);
            logger.error("❌ Error type: {}", e.getClass().getName());
            logger.error("❌ Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("❌ Caused by: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            throw e;
        }
        
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

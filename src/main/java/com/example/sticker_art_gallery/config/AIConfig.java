package com.example.sticker_art_gallery.config;

import com.example.sticker_art_gallery.service.memory.PostgresChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AIConfig.class);
    
    @Value("${spring.ai.openai.proxy.host:}")
    private String proxyHost;
    
    @Value("${spring.ai.openai.proxy.port:}")
    private String proxyPort;
    
    @Value("${spring.ai.openai.proxy.username:}")
    private String proxyUsername;
    
    @Bean
    public PostgresChatMemory postgresChatMemory() {
        return new PostgresChatMemory();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, PostgresChatMemory postgresChatMemory) {
        // Логируем настройки прокси для отладки
        LOGGER.info("🔍 OpenAI Proxy Configuration:");
        LOGGER.info("  - Host: {}", proxyHost.isEmpty() ? "NOT SET" : proxyHost);
        LOGGER.info("  - Port: {}", proxyPort.isEmpty() ? "NOT SET" : proxyPort);
        LOGGER.info("  - Username: {}", proxyUsername.isEmpty() ? "NOT SET" : proxyUsername + "***");
        
        if (proxyHost.isEmpty()) {
            LOGGER.warn("⚠️ OpenAI proxy is NOT configured - requests will go directly to OpenAI API");
        } else {
            LOGGER.info("✅ OpenAI proxy configured: {}:{}", proxyHost, proxyPort);
        }
        
        return chatClientBuilder
                .defaultAdvisors(new PromptChatMemoryAdvisor(postgresChatMemory))
                .build();
    }
} 
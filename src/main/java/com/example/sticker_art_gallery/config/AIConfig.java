package com.example.sticker_art_gallery.config;

import com.example.sticker_art_gallery.service.memory.PostgresChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    @Bean
    public PostgresChatMemory postgresChatMemory() {
        return new PostgresChatMemory();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, PostgresChatMemory postgresChatMemory) {
        return chatClientBuilder
                .defaultAdvisors(new PromptChatMemoryAdvisor(postgresChatMemory))
                .build();
    }
} 
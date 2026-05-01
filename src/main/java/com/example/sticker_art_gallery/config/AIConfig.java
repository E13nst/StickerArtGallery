package com.example.sticker_art_gallery.config;

import com.example.sticker_art_gallery.service.memory.InMemoryChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AIConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AIConfig.class);
    
    @Value("${spring.ai.openai.proxy.host:}")
    private String proxyHost;
    
    @Value("${spring.ai.openai.proxy.port:}")
    private String proxyPort;
    
    @Value("${spring.ai.openai.proxy.username:}")
    private String proxyUsername;
    
    @Value("${app.ai.context-length:10}")
    private int contextLength;
    
    @Value("${spring.ai.openai.client.connection-timeout:360000}")
    private long connectionTimeout;
    
    @Value("${spring.ai.openai.client.read-timeout:360000}")
    private long readTimeout;

    /**
     * Создает ClientHttpRequestFactory с настроенными таймаутами
     */
    @Bean
    public ClientHttpRequestFactory openAiClientHttpRequestFactory() {
        LOGGER.info("🔧 Настройка ClientHttpRequestFactory для OpenAI:");
        LOGGER.info("  - Connection timeout: {} ms ({} seconds)", connectionTimeout, connectionTimeout / 1000);
        LOGGER.info("  - Read timeout: {} ms ({} seconds)", readTimeout, readTimeout / 1000);
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectionTimeout);
        factory.setReadTimeout((int) readTimeout);
        
        LOGGER.info("✅ ClientHttpRequestFactory настроен с таймаутами");
        return factory;
    }
    
    /**
     * Создает RestClient.Builder с настроенными таймаутами для OpenAI
     */
    @Bean
    public RestClient.Builder openAiRestClientBuilder(ClientHttpRequestFactory openAiClientHttpRequestFactory) {
        LOGGER.info("🔧 Настройка RestClient.Builder для OpenAI с таймаутами");
        
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(openAiClientHttpRequestFactory);
        
        LOGGER.info("✅ RestClient.Builder настроен");
        return builder;
    }
    
    /**
     * Создает RestClient.Builder с настроенными таймаутами
     * Spring AI автоконфигурация должна использовать этот бин, если он доступен
     * Используем @Primary, чтобы этот бин имел приоритет
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        LOGGER.info("🔧 Создание RestClient.Builder с настроенными таймаутами для Spring AI");
        LOGGER.info("  - Connection timeout: {} ms ({} seconds)", connectionTimeout, connectionTimeout / 1000);
        LOGGER.info("  - Read timeout: {} ms ({} seconds)", readTimeout, readTimeout / 1000);
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectionTimeout);
        factory.setReadTimeout((int) readTimeout);
        
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory);
        
        LOGGER.info("✅ RestClient.Builder создан с настроенными таймаутами");
        return builder;
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, 
                                 @Autowired InMemoryChatMemory inMemoryChatMemory) {
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
        
        // Логируем настройки таймаута
        LOGGER.info("⏱️ OpenAI Timeout Configuration:");
        LOGGER.info("  - Connection timeout: {} ms ({} seconds)", connectionTimeout, connectionTimeout / 1000);
        LOGGER.info("  - Read timeout: {} ms ({} seconds)", readTimeout, readTimeout / 1000);
        
        LOGGER.info("✅ Using InMemoryChatMemory with context length: {}", contextLength);
        
        // В Spring AI 1.0.0 используем PromptChatMemoryAdvisor.builder() или другой способ
        // Пока используем ChatClient без advisor - память будет управляться через ChatMemory напрямую
        // В будущем можно добавить advisor, если потребуется
        return chatClientBuilder.build();
    }
} 
package com.example.dream_stream_bot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация RestTemplate для проксирования запросов
 */
@Configuration
public class RestTemplateConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateConfig.class);
    
    @Value("${sticker.processor.timeout.connect:5000}")
    private int connectTimeout;
    
    @Value("${sticker.processor.timeout.read:10000}")
    private int readTimeout;
    
    @Bean
    public RestTemplate restTemplate() {
        LOGGER.info("🔧 Настройка RestTemplate для проксирования");
        LOGGER.info("⏱️ Timeout настройки: connect={}ms, read={}ms", connectTimeout, readTimeout);
        
        // Используем SimpleClientHttpRequestFactory с timeout'ами
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        LOGGER.info("✅ RestTemplate настроен успешно");
        return restTemplate;
    }
}

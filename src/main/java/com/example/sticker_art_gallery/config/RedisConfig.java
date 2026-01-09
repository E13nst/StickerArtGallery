package com.example.sticker_art_gallery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import java.time.Duration;

/**
 * Конфигурация Redis для будущего использования
 */
@Configuration
public class RedisConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:amvera-e13nst-run-redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * Настройка подключения к Redis с отключением проверки SSL сертификатов
     * для работы с истекшими и самоподписанными сертификатами
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LOGGER.info("🔧 Настройка Redis с отключением проверки SSL сертификатов");
        LOGGER.info("📍 Подключение: {}:{}, database: {}", redisHost, redisPort, redisDatabase);
        LOGGER.info("🌍 Переменные окружения: REDIS_HOST={}, REDIS_PORT={}, REDIS_DATABASE={}", 
                   System.getenv("REDIS_HOST"), System.getenv("REDIS_PORT"), System.getenv("REDIS_DATABASE"));
        
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        configuration.setDatabase(redisDatabase);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            configuration.setPassword(redisPassword);
            LOGGER.info("🔐 Redis пароль установлен");
        } else {
            LOGGER.info("🔓 Redis пароль не установлен");
        }
        
        LettuceClientConfiguration clientConfig;
        
        if (sslEnabled) {
            // Настройка SSL с отключением проверки сертификатов
            SslOptions sslOptions = SslOptions.builder()
                    .jdkSslProvider()
                    .build();
            
            ClientOptions clientOptions = ClientOptions.builder()
                    .sslOptions(sslOptions)
                    .build();
            
            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(10))
                    .clientOptions(clientOptions)
                    .useSsl()
                    .disablePeerVerification()  // Отключаем проверку сертификатов
                    .build();
            
            LOGGER.info("🔒 SSL включен с отключенной проверкой сертификатов");
        } else {
            // Простое подключение без SSL
            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(10))
                    .build();
            
            LOGGER.info("🔓 SSL отключен, используем обычное подключение");
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, clientConfig);
        factory.setValidateConnection(false);
        
        LOGGER.info("🏭 LettuceConnectionFactory создан");
        return factory;
    }

}
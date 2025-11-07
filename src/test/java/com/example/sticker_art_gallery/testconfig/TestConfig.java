package com.example.sticker_art_gallery.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Конфигурация для тестов
 * Содержит настройки, специфичные для тестовой среды
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Константы для тестов
     */
    public static class TestConstants {
        public static final Long TEST_USER_ID = 141614461L;
        public static final String TEST_USERNAME = "test_integration_user";
        public static final String TEST_FIRST_NAME = "Test";
        public static final String TEST_LAST_NAME = "User";
        public static final String TEST_LANGUAGE_CODE = "ru";
        
        // Тестовые стикерсеты
        public static final String[] TEST_STICKER_SETS = {
            "citati_prosto", 
            "shblokun", 
            "test_stickers",
            "shaitanchick"
        };
        
        // Тестовые категории
        public static final String[] TEST_CATEGORIES = {
            "animals",
            "cute", 
            "memes",
            "nature"
        };
    }
    
    /**
     * Настройки для тестов
     */
    public static class TestSettings {
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        public static final String DEFAULT_SORT_FIELD = "createdAt";
        public static final String DEFAULT_SORT_DIRECTION = "DESC";
        public static final String DEFAULT_LANGUAGE = "en";
    }
}

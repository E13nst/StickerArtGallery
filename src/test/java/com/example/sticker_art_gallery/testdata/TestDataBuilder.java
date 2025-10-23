package com.example.sticker_art_gallery.testdata;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.util.TelegramInitDataGenerator;

/**
 * Builder для создания тестовых данных
 * Централизует создание тестовых объектов и уменьшает дублирование кода
 */
public class TestDataBuilder {
    
    // Тестовые константы
    public static final Long TEST_USER_ID = 141614461L;
    public static final String BOT_NAME = "StickerGallery";
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
    
    /**
     * Создает CreateStickerSetDto с базовыми данными
     */
    public static CreateStickerSetDto createBasicStickerSetDto() {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("test_stickers");
        return dto;
    }
    
    /**
     * Создает CreateStickerSetDto с URL стикерсета
     */
    public static CreateStickerSetDto createStickerSetDtoWithUrl(String stickerSetName) {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("https://t.me/addstickers/" + stickerSetName);
        return dto;
    }
    
    /**
     * Создает CreateStickerSetDto с кастомным title
     */
    public static CreateStickerSetDto createStickerSetDtoWithTitle(String name, String title) {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(name);
        dto.setTitle(title);
        return dto;
    }
    
    /**
     * Создает CreateStickerSetDto с кастомным userId
     */
    public static CreateStickerSetDto createStickerSetDtoWithUserId(String name, Long userId) {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(name);
        dto.setUserId(userId);
        return dto;
    }
    
    /**
     * Создает CreateStickerSetDto с некорректными данными
     */
    public static CreateStickerSetDto createInvalidStickerSetDto(String invalidName) {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(invalidName);
        return dto;
    }
    
    /**
     * Создает UserEntity для тестов
     */
    public static UserEntity createTestUser(Long userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setFirstName(TEST_FIRST_NAME);
        user.setLastName(TEST_LAST_NAME);
        user.setUsername(TEST_USERNAME);
        user.setLanguageCode(TEST_LANGUAGE_CODE);
        return user;
    }
    
    /**
     * Создает UserProfileEntity для тестов
     */
    public static UserProfileEntity createTestUserProfile(Long userId) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        profile.setRole(UserProfileEntity.UserRole.USER);
        profile.setArtBalance(0L);
        return profile;
    }
    
    /**
     * Создает валидную initData для тестов
     */
    public static String createValidInitData(String botToken, Long userId) {
        try {
            return TelegramInitDataGenerator.builder()
                    .botToken(botToken)
                    .userId(userId)
                    .username("E13nst")
                    .firstName("Andrey")
                    .lastName("Mitroshin")
                    .languageCode("ru")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create valid initData", e);
        }
    }
    
    /**
     * Создает некорректную initData для негативных тестов
     */
    public static String createInvalidInitData() {
        return "invalid_data";
    }
}

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
    public static final Long TEST_USER_ID = TestUsers.ADMIN.id();
    
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
        TestUsers.TestUser testUser = TestUsers.forId(userId);
        return TestUsers.buildUser(testUser);
    }
    
    /**
     * Создает UserProfileEntity для тестов
     */
    public static UserProfileEntity createTestUserProfile(Long userId) {
        TestUsers.TestUser testUser = TestUsers.forId(userId);
        return TestUsers.buildProfile(testUser);
    }
    
    /**
     * Создает валидную initData для тестов
     */
    public static String createValidInitData(String botToken, Long userId) {
        try {
            TestUsers.TestUser testUser = TestUsers.forId(userId);
            return TelegramInitDataGenerator.builder()
                    .botToken(botToken)
                    .userId(testUser.id())
                    .username(testUser.username())
                    .firstName(testUser.firstName())
                    .lastName(testUser.lastName())
                    .languageCode(testUser.languageCode())
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

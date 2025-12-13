package com.example.sticker_art_gallery.testdata;

/**
 * Константы для тестов
 * Централизует все тестовые константы для уменьшения дублирования и улучшения поддерживаемости
 */
public final class TestConstants {
    
    private TestConstants() {
        // Utility class
    }
    
    // Тестовые стикерсеты
    public static final String TEST_STICKERSET_CITATI_PROSTO = "citati_prosto";
    public static final String TEST_STICKERSET_SHBLOKUN = "shblokun";
    public static final String TEST_STICKERSET_TEST_STICKERS = "test_stickers";
    public static final String TEST_STICKERSET_SHAITANCHICK = "shaitanchick";
    public static final String TEST_STICKERSET_DUNEM = "Dunem";
    
    // URL тестовых стикерсетов
    public static final String TEST_STICKERSET_URL_CITATI_PROSTO = "https://t.me/addstickers/" + TEST_STICKERSET_CITATI_PROSTO;
    public static final String TEST_STICKERSET_URL_SHBLOKUN = "https://t.me/addstickers/" + TEST_STICKERSET_SHBLOKUN;
    public static final String TEST_STICKERSET_URL_TEST_STICKERS = "https://t.me/addstickers/" + TEST_STICKERSET_TEST_STICKERS;
    public static final String TEST_STICKERSET_URL_SHAITANCHICK = "https://t.me/addstickers/" + TEST_STICKERSET_SHAITANCHICK;
    public static final String TEST_STICKERSET_URL_DUNEM = "https://t.me/addstickers/" + TEST_STICKERSET_DUNEM;
    
    // Тестовые стикерсеты для фильтров
    public static final String TEST_STICKERSET_S1 = "s1_by_StickerGalleryBot";
    public static final String TEST_STICKERSET_S2 = "s2_by_StickerGalleryBot";
    public static final String TEST_STICKERSET_S3 = "s3_by_StickerGalleryBot";
    public static final String TEST_STICKERSET_BLOCKED = "blocked_by_StickerGalleryBot";
    
    // Тестовые стикерсеты для поиска
    public static final String TEST_STICKERSET_CAT = "funny_cat_stickers_test";
    public static final String TEST_STICKERSET_DOG = "happy_dogs_pack_test";
    public static final String TEST_STICKERSET_ANIMAL = "animal_kingdom_test";
    public static final String TEST_STICKERSET_PRIVATE = "private_cat_test";
    public static final String TEST_STICKERSET_BLOCKED_CAT = "blocked_cat_test";
    public static final String TEST_STICKERSET_OFFICIAL_CAT = "official_cat_test";
    
    // Тестовые стикерсеты для админ-тестов
    public static final String TEST_STICKERSET_ADMIN = "test_set_by_StickerGalleryBot";
    
    // Тестовые стикерсеты для топ-фильтров
    public static final String TEST_STICKERSET_TOP_OFFICIAL = "top_official_by_StickerGalleryBot";
    public static final String TEST_STICKERSET_TOP_AUTHORED = "top_authored_by_StickerGalleryBot";
    public static final String TEST_STICKERSET_TOP_PLAIN = "top_plain_by_StickerGalleryBot";
    
    // Тестовые стикерсеты для видимости
    public static final String TEST_STICKERSET_OWNER_PUBLIC = "owner_public_pack_by_testbot";
    public static final String TEST_STICKERSET_OWNER_PRIVATE = "owner_private_pack_by_testbot";
    public static final String TEST_STICKERSET_OWNER_BLOCKED = "owner_blocked_pack_by_testbot";
    public static final String TEST_STICKERSET_AUTHOR_PUBLIC = "author_public_pack_by_testbot";
    public static final String TEST_STICKERSET_AUTHOR_PRIVATE = "author_private_pack_by_testbot";
    public static final String TEST_STICKERSET_AUTHOR_BLOCKED = "author_blocked_pack_by_testbot";
    
    // Тестовые ID авторов
    public static final Long TEST_AUTHOR_ID_111 = 111L;
    public static final Long TEST_AUTHOR_ID_222 = 222L;
    public static final Long TEST_AUTHOR_ID_123456789 = 123456789L;
    
    // Тестовые ID пользователей для лайков
    public static final Long TEST_LIKE_USER_ID_BASE = 700000000L;
    
    // Тестовые имена пользователей
    public static final String TEST_USERNAME_E13NST = "E13nst";
    public static final String TEST_USERNAME_TEST_USER = "TestUser";
    public static final String TEST_USERNAME_TEST_INTEGRATION = "test_integration_user";
    
    // Тестовые имена
    public static final String TEST_FIRST_NAME_ANDREY = "Andrey";
    public static final String TEST_FIRST_NAME_TEST = "Test";
    
    // Тестовые фамилии
    public static final String TEST_LAST_NAME_MITROSHIN = "Mitroshin";
    public static final String TEST_LAST_NAME_USER = "User";
    
    // Языковые коды
    public static final String TEST_LANGUAGE_CODE_RU = "ru";
    public static final String TEST_LANGUAGE_CODE_EN = "en";
}


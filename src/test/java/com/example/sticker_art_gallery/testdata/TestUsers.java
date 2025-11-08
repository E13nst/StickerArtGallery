package com.example.sticker_art_gallery.testdata;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;

/**
 * Централизованное описание тестовых аккаунтов, используемых в интеграционных тестах.
 * Позволяет переиспользовать идентификаторы и атрибуты пользователей без дублирования.
 *
 * Важно: перечисленные ниже Telegram ID соответствуют реальным аккаунтам.
 * Используйте их только в тестовом окружении, не изменяйте значения без согласования.
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static final TestUser ADMIN = new TestUser(
            141_614_461L,
            "test_admin_user",
            "AdminTest",
            "User",
            "ru",
            UserProfileEntity.UserRole.USER // роль будет повышена в тестах при необходимости
    );

    public static final TestUser VIEWER = new TestUser(
            8_458_436_384L,
            "test_viewer_user",
            "ViewerTest",
            "User",
            "ru",
            UserProfileEntity.UserRole.USER
    );

    public static final TestUser OWNER = new TestUser(
            777_000_001L,
            "test_owner_user",
            "OwnerTest",
            "User",
            "ru",
            UserProfileEntity.UserRole.USER
    );

    /**
     * Возвращает метаданные тестового пользователя по ID. Если ID не зарегистрирован,
     * создаёт временное описание пользователя с дефолтными значениями.
     */
    public static TestUser forId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (ADMIN.id().equals(userId)) {
            return ADMIN;
        }
        if (OWNER.id().equals(userId)) {
            return OWNER;
        }
        if (VIEWER.id().equals(userId)) {
            return VIEWER;
        }
        return new TestUser(
                userId,
                "test_user_" + userId,
                "Test",
                "User",
                "ru",
                UserProfileEntity.UserRole.USER
        );
    }

    /**
     * Создаёт сущность пользователя для указанного тестового аккаунта.
     */
    public static UserEntity buildUser(TestUser user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setLanguageCode(user.languageCode());
        return entity;
    }

    /**
     * Создаёт профиль пользователя для указанного тестового аккаунта.
     */
    public static UserProfileEntity buildProfile(TestUser user) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(user.id());
        profile.setRole(user.defaultRole());
        profile.setIsBlocked(false);
        profile.setArtBalance(0L);
        return profile;
    }

    /**
     * Структура с данными тестового пользователя.
     */
    public record TestUser(
            Long id,
            String username,
            String firstName,
            String lastName,
            String languageCode,
            UserProfileEntity.UserRole defaultRole
    ) {
    }
}


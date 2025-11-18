package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Управление стикерсетами")
@Feature("Доступные действия для стикерсетов")
@DisplayName("Тесты доступных действий для StickerSetDto")
class StickerSetDtoAvailableActionsTest {

    private static final Long OWNER_USER_ID = 123L;
    private static final Long OTHER_USER_ID = 456L;
    private static final Long ADMIN_USER_ID = 999L;

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец публичного стикерсета должен видеть DELETE и MAKE_PRIVATE")
    @Description("Проверяет, что владелец публичного не заблокированного стикерсета может удалить его и сделать приватным")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerPublicStickerSet_ShouldReturnDeleteAndMakePrivate() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, true, false
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.MAKE_PRIVATE));
        assertFalse(actions.contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец приватного стикерсета должен видеть DELETE и MAKE_PUBLIC")
    @Description("Проверяет, что владелец приватного не заблокированного стикерсета может удалить его и сделать публичным")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerPrivateStickerSet_ShouldReturnDeleteAndMakePublic() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, false, false
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(actions.contains(StickerSetAction.MAKE_PRIVATE));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ для незаблокированного стикерсета должен видеть BLOCK")
    @Description("Проверяет, что администратор для незаблокированного стикерсета может заблокировать его")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminNotBlockedStickerSet_ShouldReturnBlock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, OWNER_USER_ID, true, false
        );

        // Then
        assertEquals(1, actions.size());
        assertTrue(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
        assertFalse(actions.contains(StickerSetAction.DELETE));
        assertFalse(actions.contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(actions.contains(StickerSetAction.MAKE_PRIVATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ для заблокированного стикерсета должен видеть UNBLOCK")
    @Description("Проверяет, что администратор для заблокированного стикерсета может разблокировать его")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminBlockedStickerSet_ShouldReturnUnblock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, OWNER_USER_ID, true, true
        );

        // Then
        assertEquals(1, actions.size());
        assertTrue(actions.contains(StickerSetAction.UNBLOCK));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.DELETE));
        assertFalse(actions.contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(actions.contains(StickerSetAction.MAKE_PRIVATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ-владелец публичного стикерсета должен видеть DELETE, MAKE_PRIVATE и BLOCK")
    @Description("Проверяет, что администратор, который является владельцем публичного стикерсета, видит все доступные ему действия")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminOwnerPublicStickerSet_ShouldReturnDeleteMakePrivateAndBlock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, ADMIN_USER_ID, true, false
        );

        // Then
        assertEquals(3, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.MAKE_PRIVATE));
        assertTrue(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ-владелец заблокированного стикерсета должен видеть DELETE, MAKE_PRIVATE и UNBLOCK")
    @Description("Проверяет, что администратор, который является владельцем заблокированного стикерсета, видит все доступные ему действия")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminOwnerBlockedStickerSet_ShouldReturnDeleteMakePrivateAndUnblock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, ADMIN_USER_ID, true, true
        );

        // Then
        assertEquals(3, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.MAKE_PRIVATE));
        assertTrue(actions.contains(StickerSetAction.UNBLOCK));
        assertFalse(actions.contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Чужой пользователь не должен видеть никаких действий")
    @Description("Проверяет, что обычный пользователь не видит действий для чужих стикерсетов")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OtherUser_ShouldReturnEmpty() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OTHER_USER_ID, false, OWNER_USER_ID, true, false
        );

        // Then
        assertTrue(actions.isEmpty());
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Неавторизованный пользователь не должен видеть никаких действий")
    @Description("Проверяет, что неавторизованный пользователь (currentUserId = null) не видит действий")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_UnauthorizedUser_ShouldReturnEmpty() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                null, false, OWNER_USER_ID, true, false
        );

        // Then
        assertTrue(actions.isEmpty());
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ без прав админа не должен видеть действий блокировки")
    @Description("Проверяет, что если isAdmin = false, то даже для админского userId не показываются действия блокировки")
    @Severity(SeverityLevel.NORMAL)
    void calculateAvailableActions_AdminUserIdButNotAdminRole_ShouldNotReturnBlockActions() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, false, OWNER_USER_ID, true, false
        );

        // Then
        assertTrue(actions.isEmpty());
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity без currentUserId должен устанавливать пустой список действий")
    @Description("Проверяет, что при создании DTO без currentUserId список доступных действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithoutCurrentUserId_ShouldHaveEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с currentUserId владельца должен рассчитывать действия владельца")
    @Description("Проверяет, что при создании DTO с currentUserId владельца рассчитываются правильные действия")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithOwnerCurrentUserId_ShouldCalculateOwnerActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OWNER_USER_ID);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(2, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.MAKE_PRIVATE));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с админом должен рассчитывать действия админа")
    @Description("Проверяет, что при создании DTO с isAdmin = true рассчитываются действия администратора")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithAdmin_ShouldCalculateAdminActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", ADMIN_USER_ID, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(1, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с админом-владельцем должен рассчитывать все действия")
    @Description("Проверяет, что при создании DTO с админом, который является владельцем, рассчитываются все доступные действия")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithAdminOwner_ShouldCalculateAllActions() {
        // Given
        StickerSet entity = createStickerSet(ADMIN_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", ADMIN_USER_ID, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(3, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.MAKE_PRIVATE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с заблокированным стикерсетом должен показывать UNBLOCK для админа")
    @Description("Проверяет, что для заблокированного стикерсета админ видит UNBLOCK вместо BLOCK")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithBlockedStickerSet_ShouldShowUnblockForAdmin() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", ADMIN_USER_ID, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(1, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNBLOCK));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с приватным стикерсетом должен показывать MAKE_PUBLIC для владельца")
    @Description("Проверяет, что для приватного стикерсета владелец видит MAKE_PUBLIC вместо MAKE_PRIVATE")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithPrivateStickerSet_ShouldShowMakePublicForOwner() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, false, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OWNER_USER_ID, false);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(2, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.MAKE_PUBLIC));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.MAKE_PRIVATE));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с чужим пользователем должен устанавливать пустой список действий")
    @Description("Проверяет, что при создании DTO с currentUserId другого пользователя список действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithOtherUser_ShouldHaveEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OTHER_USER_ID, false);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }

    /**
     * Вспомогательный метод для создания StickerSet для тестов
     */
    private StickerSet createStickerSet(Long userId, Boolean isPublic, Boolean isBlocked) {
        StickerSet entity = new StickerSet();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setTitle("Test StickerSet");
        entity.setName("test_stickers_by_bot");
        entity.setIsPublic(isPublic);
        entity.setIsBlocked(isBlocked);
        entity.setIsOfficial(false);
        entity.setLikesCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}


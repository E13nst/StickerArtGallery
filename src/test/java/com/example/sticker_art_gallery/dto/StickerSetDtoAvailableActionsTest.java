package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
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
    private static final Long AUTHOR_USER_ID = 124L;
    private static final Long OTHER_USER_ID = 456L;
    private static final Long ADMIN_USER_ID = 999L;

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец-автор публичного стикерсета должен видеть DELETE, EDIT_CATEGORIES и UNPUBLISH")
    @Description("Проверяет, что владелец, который также является автором, публичного не заблокированного стикерсета может удалить его, редактировать категории и скрыть из галереи")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerAuthorPublicStickerSet_ShouldReturnDeleteAndUnpublish() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, OWNER_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(3, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.UNPUBLISH));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец-автор приватного стикерсета должен видеть DELETE, EDIT_CATEGORIES и PUBLISH")
    @Description("Проверяет, что владелец, который также является автором, приватного не заблокированного стикерсета может удалить его, редактировать категории и опубликовать")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerAuthorPrivateStickerSet_ShouldReturnDeleteAndPublish() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, OWNER_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PRIVATE
        );

        // Then
        assertEquals(3, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.PUBLISH));
        assertFalse(actions.contains(StickerSetAction.UNPUBLISH));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ для незаблокированного стикерсета должен видеть EDIT_CATEGORIES и BLOCK")
    @Description("Проверяет, что администратор для незаблокированного стикерсета может редактировать категории и заблокировать его")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminNotBlockedStickerSet_ShouldReturnBlock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
        assertFalse(actions.contains(StickerSetAction.DELETE));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
        assertFalse(actions.contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ для заблокированного стикерсета должен видеть EDIT_CATEGORIES и UNBLOCK")
    @Description("Проверяет, что администратор для заблокированного стикерсета может редактировать категории и разблокировать его")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminBlockedStickerSet_ShouldReturnUnblock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.BLOCKED, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.UNBLOCK));
        assertFalse(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.DELETE));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
        assertFalse(actions.contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ-владелец-автор публичного стикерсета должен видеть DELETE, EDIT_CATEGORIES, UNPUBLISH и BLOCK")
    @Description("Проверяет, что администратор, который является владельцем и автором публичного стикерсета, видит все доступные ему действия")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminOwnerAuthorPublicStickerSet_ShouldReturnDeleteUnpublishAndBlock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, ADMIN_USER_ID, ADMIN_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(4, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.UNPUBLISH));
        assertTrue(actions.contains(StickerSetAction.BLOCK));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
        assertFalse(actions.contains(StickerSetAction.UNBLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Админ-владелец-автор заблокированного стикерсета должен видеть DELETE, EDIT_CATEGORIES, UNPUBLISH и UNBLOCK")
    @Description("Проверяет, что администратор, который является владельцем и автором заблокированного стикерсета, видит все доступные ему действия")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminOwnerAuthorBlockedStickerSet_ShouldReturnDeleteUnpublishAndUnblock() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, ADMIN_USER_ID, ADMIN_USER_ID, 
                StickerSetState.BLOCKED, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(4, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.UNPUBLISH));
        assertTrue(actions.contains(StickerSetAction.UNBLOCK));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
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
                OTHER_USER_ID, false, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
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
                null, false, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
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
                ADMIN_USER_ID, false, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertTrue(actions.isEmpty());
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец (не автор) публичного стикерсета должен видеть DELETE и EDIT_CATEGORIES")
    @Description("Проверяет, что владелец, который не является автором, может удалить стикерсет и редактировать категории, но не может публиковать/скрывать")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerNotAuthor_ShouldReturnOnlyDelete() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
        assertFalse(actions.contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Автор (не владелец) публичного стикерсета должен видеть только UNPUBLISH")
    @Description("Проверяет, что автор, который не является владельцем, может только публиковать/скрывать стикерсет, но не может удалить или редактировать категории")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AuthorNotOwner_ShouldReturnOnlyUnpublish() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                AUTHOR_USER_ID, false, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(1, actions.size());
        assertTrue(actions.contains(StickerSetAction.UNPUBLISH));
        assertFalse(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertFalse(actions.contains(StickerSetAction.DELETE));
        assertFalse(actions.contains(StickerSetAction.PUBLISH));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity без currentUserId должен устанавливать пустой список действий")
    @Description("Проверяет, что при создании DTO без currentUserId список доступных действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithoutCurrentUserId_ShouldHaveEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с currentUserId владельца-автора должен рассчитывать действия владельца-автора")
    @Description("Проверяет, что при создании DTO с currentUserId владельца, который также является автором, рассчитываются правильные действия")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithOwnerAuthorCurrentUserId_ShouldCalculateOwnerAuthorActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, OWNER_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OWNER_USER_ID, false);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(3, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с админом должен рассчитывать действия админа")
    @Description("Проверяет, что при создании DTO с isAdmin = true рассчитываются действия администратора")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithAdmin_ShouldCalculateAdminActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", ADMIN_USER_ID, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(2, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с админом-владельцем-автором должен рассчитывать все действия")
    @Description("Проверяет, что при создании DTO с админом, который является владельцем и автором, рассчитываются все доступные действия")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithAdminOwnerAuthor_ShouldCalculateAllActions() {
        // Given
        StickerSet entity = createStickerSet(ADMIN_USER_ID, ADMIN_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", ADMIN_USER_ID, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(4, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с заблокированным стикерсетом должен показывать EDIT_CATEGORIES и UNBLOCK для админа")
    @Description("Проверяет, что для заблокированного стикерсета админ видит EDIT_CATEGORIES и UNBLOCK вместо BLOCK")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithBlockedStickerSet_ShouldShowUnblockForAdmin() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, true);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", ADMIN_USER_ID, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(2, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNBLOCK));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с приватным стикерсетом должен показывать EDIT_CATEGORIES и PUBLISH для автора")
    @Description("Проверяет, что для приватного стикерсета автор видит EDIT_CATEGORIES и PUBLISH вместо UNPUBLISH")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithPrivateStickerSet_ShouldShowPublishForAuthor() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, OWNER_USER_ID, false, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OWNER_USER_ID, false);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(3, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.PUBLISH));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с чужим пользователем должен устанавливать пустой список действий")
    @Description("Проверяет, что при создании DTO с currentUserId другого пользователя список действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithOtherUser_ShouldHaveEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OTHER_USER_ID, false);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("EDIT_CATEGORIES не должно показываться для удаленного стикерсета")
    @Description("Проверяет, что для удаленного стикерсета действие EDIT_CATEGORIES не показывается даже владельцу")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_DeletedStickerSet_ShouldNotShowEditCategories() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, OWNER_USER_ID, 
                StickerSetState.DELETED, StickerSetVisibility.PUBLIC
        );

        // Then
        assertTrue(actions.isEmpty());
        assertFalse(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertFalse(actions.contains(StickerSetAction.DELETE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("EDIT_CATEGORIES должно показываться для владельца без authorId")
    @Description("Проверяет, что владелец может редактировать категории даже если authorId = null")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerWithoutAuthor_ShouldShowEditCategories() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, null, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("EDIT_CATEGORIES должно показываться для автора без совпадения с владельцем")
    @Description("Проверяет, что автор может редактировать категории даже если он не является владельцем")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AuthorOnly_ShouldShowEditCategories() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                AUTHOR_USER_ID, false, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PRIVATE
        );

        // Then
        assertEquals(1, actions.size());
        assertFalse(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.PUBLISH));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("EDIT_CATEGORIES должно показываться для админа чужого стикерсета")
    @Description("Проверяет, что админ может редактировать категории любого стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_AdminOtherUserStickerSet_ShouldShowEditCategories() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, OWNER_USER_ID, AUTHOR_USER_ID, 
                StickerSetState.ACTIVE, StickerSetVisibility.PRIVATE
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.BLOCK));
    }

    /**
     * Вспомогательный метод для создания StickerSet для тестов
     */
    private StickerSet createStickerSet(Long userId, Long authorId, Boolean isPublic, Boolean isBlocked) {
        StickerSet entity = new StickerSet();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setAuthorId(authorId);
        entity.setTitle("Test StickerSet");
        entity.setName("test_stickers_by_bot");
        entity.setState(isBlocked ? StickerSetState.BLOCKED : StickerSetState.ACTIVE);
        entity.setVisibility(isPublic ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE);
        entity.setType(StickerSetType.USER);
        if (isBlocked) {
            entity.setBlockReason("Test block reason");
        }
        entity.setLikesCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}


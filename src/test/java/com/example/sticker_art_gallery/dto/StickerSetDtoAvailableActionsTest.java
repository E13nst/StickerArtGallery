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
                OWNER_USER_ID, false, OWNER_USER_ID, true, 
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
                OWNER_USER_ID, false, OWNER_USER_ID, true, 
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
                ADMIN_USER_ID, true, OWNER_USER_ID, true, 
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
                ADMIN_USER_ID, true, OWNER_USER_ID, true, 
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
                ADMIN_USER_ID, true, ADMIN_USER_ID, true, 
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
                ADMIN_USER_ID, true, ADMIN_USER_ID, true, 
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
                OTHER_USER_ID, false, OWNER_USER_ID, true, 
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
                null, false, OWNER_USER_ID, true, 
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
                ADMIN_USER_ID, false, OWNER_USER_ID, true, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC
        );

        // Then
        assertTrue(actions.isEmpty());
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец без isVerified видит только DELETE и EDIT_CATEGORIES")
    @Description("Проверяет, что владелец при isVerified=false может удалить и редактировать категории, но не PUBLISH/UNPUBLISH")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_OwnerNotVerified_ShouldReturnDeleteAndEditCategories() {
        // When - owner, isVerified=false (author=owner only when verified)
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, false,
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
    @Story("Методы fromEntity")
    @DisplayName("fromEntity без currentUserId должен устанавливать пустой список действий")
    @Description("Проверяет, что при создании DTO без currentUserId список доступных действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithoutCurrentUserId_ShouldHaveEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

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
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

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
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

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
        StickerSet entity = createStickerSet(ADMIN_USER_ID, true, true, false);

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
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, true);

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
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, false, false);

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
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

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
                OWNER_USER_ID, false, OWNER_USER_ID, true, 
                StickerSetState.DELETED, StickerSetVisibility.PUBLIC
        );

        // Then
        assertTrue(actions.isEmpty());
        assertFalse(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertFalse(actions.contains(StickerSetAction.DELETE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("EDIT_CATEGORIES должно показываться для владельца без isVerified")
    @Description("Проверяет, что владелец может редактировать категории даже если isVerified = false")
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
    @DisplayName("PUBLISH/UNPUBLISH показываются для верифицированного владельца")
    @Description("Проверяет, что владелец с isVerified=true видит PUBLISH для приватного стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_VerifiedOwner_ShouldShowPublish() {
        // When - owner with isVerified (author = owner)
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PRIVATE
        );

        // Then
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
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
                ADMIN_USER_ID, true, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PRIVATE
        );

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("DONATE должно показываться для пользователя с кошельком, если isVerified и пользователь не владелец")
    @Description("Проверяет, что пользователь с TON кошельком видит DONATE для верифицированного стикерсета, если он не владелец")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_UserWithWalletAndVerified_ShouldShowDonation() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OTHER_USER_ID, false, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, true
        );

        // Then
        assertEquals(1, actions.size());
        assertTrue(actions.contains(StickerSetAction.DONATE));
        assertFalse(actions.contains(StickerSetAction.DELETE));
        assertFalse(actions.contains(StickerSetAction.EDIT_CATEGORIES));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("DONATE не должно показываться, если пользователь является владельцем (автором)")
    @Description("Проверяет, что владелец не может донатить самому себе")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_UserIsOwner_ShouldNotShowDonation() {
        // When - owner with isVerified
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, true
        );

        // Then
        assertTrue(actions.contains(StickerSetAction.UNPUBLISH));
        assertFalse(actions.contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("DONATE не должно показываться, если у пользователя нет кошелька")
    @Description("Проверяет, что DONATE не показывается, если hasTonWallet = false")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_UserWithoutWallet_ShouldNotShowDonation() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OTHER_USER_ID, false, OWNER_USER_ID, true, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, false
        );

        // Then
        assertTrue(actions.isEmpty());
        assertFalse(actions.contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("DONATE не должно показываться, если у стикерсета нет автора")
    @Description("Проверяет, что DONATE не показывается, если isVerified = false")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_StickerSetWithoutAuthor_ShouldNotShowDonation() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OTHER_USER_ID, false, OWNER_USER_ID, null, 
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, true
        );

        // Then
        assertTrue(actions.isEmpty());
        assertFalse(actions.contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("DONATE не должно показываться для неавторизованного пользователя")
    @Description("Проверяет, что DONATE не показывается, если currentUserId = null")
    @Severity(SeverityLevel.CRITICAL)
    void calculateAvailableActions_UnauthorizedUserWithWallet_ShouldNotShowDonation() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                null, false, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, true
        );

        // Then
        assertTrue(actions.isEmpty());
        assertFalse(actions.contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("Владелец получает DELETE и EDIT_CATEGORIES, но не DONATE")
    @Description("Проверяет, что владелец не видит DONATE (нельзя донатить себе)")
    @Severity(SeverityLevel.NORMAL)
    void calculateAvailableActions_OwnerWithWallet_ShouldNotShowDonation() {
        // When - owner never sees DONATE
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                OWNER_USER_ID, false, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, true
        );

        // Then
        assertTrue(actions.contains(StickerSetAction.DELETE));
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertFalse(actions.contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Расчет доступных действий")
    @DisplayName("DONATE должно показываться для админа с кошельком, если он не является автором")
    @Description("Проверяет, что админ с кошельком видит DONATE вместе с админскими действиями, если он не является автором")
    @Severity(SeverityLevel.NORMAL)
    void calculateAvailableActions_AdminWithWalletNotAuthor_ShouldShowDonationAndAdminActions() {
        // When
        List<StickerSetAction> actions = StickerSetDto.calculateAvailableActions(
                ADMIN_USER_ID, true, OWNER_USER_ID, true,
                StickerSetState.ACTIVE, StickerSetVisibility.PUBLIC, true
        );

        // Then
        assertEquals(3, actions.size());
        assertTrue(actions.contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(actions.contains(StickerSetAction.BLOCK));
        assertTrue(actions.contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с hasTonWallet=true должен включать DONATE для пользователя с кошельком")
    @Description("Проверяет, что при создании DTO с hasTonWallet=true и наличием автора, пользователь видит DONATE")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithWalletAndVerified_ShouldIncludeDonation() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OTHER_USER_ID, false, true, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertEquals(1, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с hasTonWallet=false не должен включать DONATE")
    @Description("Проверяет, что при создании DTO с hasTonWallet=false DONATE не показывается")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithoutWallet_ShouldNotIncludeDonation() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OTHER_USER_ID, false, true, false);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с hasTonWallet=true не должен включать DONATE, если пользователь является автором")
    @Description("Проверяет, что даже с hasTonWallet=true DONATE не показывается, если пользователь является автором")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithWalletButUserIsOwner_ShouldNotIncludeDonation() {
        // Given - owner = author when verified
        StickerSet entity = createStickerSet(OWNER_USER_ID, true, true, false);

        // When - current user is owner
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OWNER_USER_ID, false, true, true);

        // Then - owner never sees DONATE
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.DONATE));
    }

    @Test
    @Story("Методы fromEntity")
    @DisplayName("fromEntity с hasTonWallet=true не должен включать DONATE, если нет автора")
    @Description("Проверяет, что даже с hasTonWallet=true DONATE не показывается, если isVerified = false")
    @Severity(SeverityLevel.CRITICAL)
    void fromEntity_WithWalletButNoAuthor_ShouldNotIncludeDonation() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, false, true, false);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity, "en", OTHER_USER_ID, false, true, true);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.DONATE));
    }

    /**
     * Вспомогательный метод для создания StickerSet для тестов
     */
    private StickerSet createStickerSet(Long userId, Boolean isVerified, Boolean isPublic, Boolean isBlocked) {
        StickerSet entity = new StickerSet();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setIsVerified(Boolean.TRUE.equals(isVerified));
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


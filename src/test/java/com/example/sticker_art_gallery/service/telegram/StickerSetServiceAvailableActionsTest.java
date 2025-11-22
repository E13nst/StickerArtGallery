package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.StickerSetAction;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.service.category.CategoryService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Управление стикерсетами")
@Feature("Доступные действия в StickerSetService")
@DisplayName("Тесты доступных действий в StickerSetService")
class StickerSetServiceAvailableActionsTest {

    @Mock
    private StickerSetRepository stickerSetRepository;

    @Mock
    private TelegramBotApiService telegramBotApiService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ArtRewardService artRewardService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StickerSetService stickerSetService;

    private static final Long OWNER_USER_ID = 123L;
    private static final Long AUTHOR_USER_ID = 124L;
    private static final Long ADMIN_USER_ID = 999L;
    private static final Long OTHER_USER_ID = 456L;

    @BeforeEach
    void setUp() {
        // Очищаем SecurityContext перед каждым тестом
        SecurityContextHolder.clearContext();
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData для владельца-автора должен рассчитывать действия владельца-автора")
    @Description("Проверяет, что при получении стикерсета владельцем, который также является автором, рассчитываются правильные действия")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_ForOwnerAuthor_ShouldCalculateOwnerAuthorActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, OWNER_USER_ID, true, false);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Мокаем SecurityContext для неадмина
        setupSecurityContext(OWNER_USER_ID, false);

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", OWNER_USER_ID, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertEquals(3, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData для админа должен рассчитывать действия админа")
    @Description("Проверяет, что при получении стикерсета админом рассчитываются действия администратора")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_ForAdmin_ShouldCalculateAdminActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, false);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Мокаем SecurityContext для админа
        setupSecurityContext(ADMIN_USER_ID, true);

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", ADMIN_USER_ID, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertEquals(2, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData для админа-владельца-автора должен рассчитывать все действия")
    @Description("Проверяет, что при получении стикерсета админом, который является владельцем и автором, рассчитываются все действия")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_ForAdminOwnerAuthor_ShouldCalculateAllActions() {
        // Given
        StickerSet entity = createStickerSet(ADMIN_USER_ID, ADMIN_USER_ID, true, false);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Мокаем SecurityContext для админа
        setupSecurityContext(ADMIN_USER_ID, true);

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", ADMIN_USER_ID, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertEquals(4, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData для чужого пользователя должен возвращать пустой список действий")
    @Description("Проверяет, что при получении чужого стикерсета список действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_ForOtherUser_ShouldReturnEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, false);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Мокаем SecurityContext для обычного пользователя
        setupSecurityContext(OTHER_USER_ID, false);

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", OTHER_USER_ID, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData для заблокированного стикерсета должен показывать UNBLOCK для админа")
    @Description("Проверяет, что для заблокированного стикерсета админ видит UNBLOCK вместо BLOCK")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_ForBlockedStickerSet_ShouldShowUnblockForAdmin() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, true);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Мокаем SecurityContext для админа
        setupSecurityContext(ADMIN_USER_ID, true);

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", ADMIN_USER_ID, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertEquals(2, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.UNBLOCK));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.BLOCK));
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData для приватного стикерсета должен показывать PUBLISH для автора")
    @Description("Проверяет, что для приватного стикерсета автор видит PUBLISH вместо UNPUBLISH")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_ForPrivateStickerSet_ShouldShowPublishForAuthor() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, OWNER_USER_ID, false, false);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Мокаем SecurityContext для владельца-автора
        setupSecurityContext(OWNER_USER_ID, false);

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", OWNER_USER_ID, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertEquals(3, dto.getAvailableActions().size());
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.DELETE));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.EDIT_CATEGORIES));
        assertTrue(dto.getAvailableActions().contains(StickerSetAction.PUBLISH));
        assertFalse(dto.getAvailableActions().contains(StickerSetAction.UNPUBLISH));
    }

    @Test
    @Story("Методы обогащения")
    @DisplayName("findByIdWithBotApiData без currentUserId должен возвращать пустой список действий")
    @Description("Проверяет, что при получении стикерсета без авторизации список действий пуст")
    @Severity(SeverityLevel.CRITICAL)
    void findByIdWithBotApiData_WithoutCurrentUserId_ShouldReturnEmptyActions() {
        // Given
        StickerSet entity = createStickerSet(OWNER_USER_ID, AUTHOR_USER_ID, true, false);
        when(stickerSetRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        
        // Не устанавливаем SecurityContext

        // When
        StickerSetDto dto = stickerSetService.findByIdWithBotApiData(1L, "en", null, false);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }

    /**
     * Вспомогательный метод для настройки SecurityContext
     */
    private void setupSecurityContext(Long userId, boolean isAdmin) {
        // Настраиваем authentication
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn(String.valueOf(userId));
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        lenient().when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        
        // Настраиваем securityContext
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Устанавливаем реальный SecurityContext через SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);
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
        entity.setCreatedAt(java.time.LocalDateTime.now());
        return entity;
    }
}


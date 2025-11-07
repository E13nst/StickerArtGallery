package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.user.UserService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Disabled("Требует обновления после рефакторинга User/UserProfile")
@ExtendWith(MockitoExtension.class)
@Epic("Бизнес-логика стикерсетов")
@Feature("Сервис управления стикерсетами")
@DisplayName("Тесты StickerSetService")
class StickerSetServiceTest {

    @Mock
    private StickerSetRepository stickerSetRepository;

    @Mock
    private UserService userService;

    @Mock
    private TelegramBotApiService telegramBotApiService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StickerSetService stickerSetService;

    private UserEntity testUser;
    private StickerSet existingStickerSet;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(141614461L);
        testUser.setFirstName("Andrey");
        testUser.setLastName("Mitroshin");
        testUser.setUsername("E13nst");

        existingStickerSet = new StickerSet();
        existingStickerSet.setId(1L);
        existingStickerSet.setName("existing_sticker_set");
        existingStickerSet.setTitle("Existing Sticker Set");
        existingStickerSet.setUserId(141614461L);
    }

    @Disabled("Проблемы с моками SecurityContextHolder")
    @Test
    @Story("Создание стикерсета")
    @DisplayName("createStickerSet с новым стикерсетом должен успешно создать стикерсет")
    @Description("Проверяет, что сервис корректно создает новый стикерсет: " +
                "1) Извлекает имя из URL; " +
                "2) Проверяет отсутствие дубликатов; " +
                "3) Валидирует через Telegram API; " +
                "4) Сохраняет в БД")
    @Severity(SeverityLevel.BLOCKER)
    void createStickerSet_WithNewStickerSet_ShouldCreateSuccessfully() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/ShaitanChick");

        // Настраиваем моки
        when(stickerSetRepository.findByName("shaitanchick")).thenReturn(Optional.empty());
        
        Object telegramStickerSetInfo = createMockTelegramStickerSetInfo("Shaitan Chick");
        when(telegramBotApiService.validateStickerSetExists("shaitanchick"))
                .thenReturn(telegramStickerSetInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo))
                .thenReturn("Shaitan Chick");

        // Настраиваем SecurityContext
        when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // when(userService.findOrCreateByTelegramId(eq(141614461L), any(), any(), any(), any()))
        //         .thenReturn(testUser);

        StickerSet savedStickerSet = createMockSavedStickerSet("shaitanchick", "Shaitan Chick", 141614461L);
        when(stickerSetRepository.save(any(StickerSet.class))).thenReturn(savedStickerSet);

        // When
        StickerSet result = stickerSetService.createStickerSet(createDto);

        // Then
        assertNotNull(result);
        assertEquals("shaitanchick", result.getName());
        assertEquals("Shaitan Chick", result.getTitle());
        assertEquals(141614461L, result.getUserId());

        verify(stickerSetRepository).findByName("shaitanchick");
        verify(telegramBotApiService).validateStickerSetExists("shaitanchick");
        verify(telegramBotApiService).extractTitleFromStickerSetInfo(telegramStickerSetInfo);
        // verify(userService).findOrCreateByTelegramId(eq(141614461L), any(), any(), any(), any());
        verify(stickerSetRepository).save(any(StickerSet.class));
    }

    @Test
    @DisplayName("createStickerSet с существующим стикерсетом должен выбросить исключение")
    void createStickerSet_WithExistingStickerSet_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("existing_sticker_set");

        when(stickerSetRepository.findByName("existing_sticker_set"))
                .thenReturn(Optional.of(existingStickerSet));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });

        assertEquals("Стикерсет с именем 'existing_sticker_set' уже существует в галерее", 
                exception.getMessage());

        verify(stickerSetRepository).findByName("existing_sticker_set");
        verify(telegramBotApiService, never()).validateStickerSetExists(any());
        verify(stickerSetRepository, never()).save(any());
    }

    @Test
    @DisplayName("createStickerSet с несуществующим стикерсетом в Telegram должен выбросить исключение")
    void createStickerSet_WithNonExistentStickerSetInTelegram_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("nonexistent_sticker_set");

        when(stickerSetRepository.findByName("nonexistent_sticker_set"))
                .thenReturn(Optional.empty());
        when(telegramBotApiService.validateStickerSetExists("nonexistent_sticker_set"))
                .thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });

        assertTrue(exception.getMessage().contains("Стикерсет 'nonexistent_sticker_set' не найден в Telegram"));

        verify(stickerSetRepository).findByName("nonexistent_sticker_set");
        verify(telegramBotApiService).validateStickerSetExists("nonexistent_sticker_set");
        verify(stickerSetRepository, never()).save(any());
    }

    @Disabled("Проблемы с моками SecurityContextHolder")
    @Test
    @DisplayName("createStickerSet с указанным title должен использовать его вместо извлечения из Telegram API")
    void createStickerSet_WithProvidedTitle_ShouldUseProvidedTitle() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");
        createDto.setTitle("Custom Title");

        when(stickerSetRepository.findByName("test_stickers")).thenReturn(Optional.empty());
        
        Object telegramStickerSetInfo = createMockTelegramStickerSetInfo("Telegram Title");
        when(telegramBotApiService.validateStickerSetExists("test_stickers"))
                .thenReturn(telegramStickerSetInfo);

        // Настраиваем SecurityContext
        when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // when(userService.findOrCreateByTelegramId(eq(141614461L), any(), any(), any(), any()))
        //         .thenReturn(testUser);

        StickerSet savedStickerSet = createMockSavedStickerSet("test_stickers", "Custom Title", 141614461L);
        when(stickerSetRepository.save(any(StickerSet.class))).thenReturn(savedStickerSet);

        // When
        StickerSet result = stickerSetService.createStickerSet(createDto);

        // Then
        assertNotNull(result);
        assertEquals("Custom Title", result.getTitle());

        verify(telegramBotApiService).validateStickerSetExists("test_stickers");
        verify(telegramBotApiService, never()).extractTitleFromStickerSetInfo(any());
    }

    @Disabled("Проблемы с моками SecurityContextHolder")
    @Test
    @DisplayName("createStickerSet без userId и без аутентификации должен выбросить исключение")
    void createStickerSet_WithoutUserIdAndAuthentication_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");

        when(stickerSetRepository.findByName("test_stickers")).thenReturn(Optional.empty());
        
        Object telegramStickerSetInfo = createMockTelegramStickerSetInfo("Test Stickers");
        when(telegramBotApiService.validateStickerSetExists("test_stickers"))
                .thenReturn(telegramStickerSetInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo))
                .thenReturn("Test Stickers");

        // Настраиваем SecurityContext для возврата null
        when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });

        assertEquals("Не удалось определить ID пользователя. Укажите userId или убедитесь, что вы авторизованы через Telegram Web App", 
                exception.getMessage());
    }

    @Test
    @DisplayName("createStickerSet с ошибкой при обращении к Telegram API должен выбросить исключение")
    void createStickerSet_WithTelegramApiError_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");

        when(stickerSetRepository.findByName("test_stickers")).thenReturn(Optional.empty());
        when(telegramBotApiService.validateStickerSetExists("test_stickers"))
                .thenThrow(new RuntimeException("Network error"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });

        assertEquals("Не удалось проверить существование стикерсета в Telegram: Network error", 
                exception.getMessage());
    }

    @Disabled("Проблемы с моками SecurityContextHolder")
    @Test
    @DisplayName("createStickerSet с URL должен правильно извлечь имя стикерсета")
    void createStickerSet_WithUrl_ShouldExtractStickerSetName() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/ShaitanChick");

        when(stickerSetRepository.findByName("shaitanchick")).thenReturn(Optional.empty());
        
        Object telegramStickerSetInfo = createMockTelegramStickerSetInfo("Shaitan Chick");
        when(telegramBotApiService.validateStickerSetExists("shaitanchick"))
                .thenReturn(telegramStickerSetInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo))
                .thenReturn("Shaitan Chick");

        // Настраиваем SecurityContext
        when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // when(userService.findOrCreateByTelegramId(eq(141614461L), any(), any(), any(), any()))
        //         .thenReturn(testUser);

        StickerSet savedStickerSet = createMockSavedStickerSet("shaitanchick", "Shaitan Chick", 141614461L);
        when(stickerSetRepository.save(any(StickerSet.class))).thenReturn(savedStickerSet);

        // When
        StickerSet result = stickerSetService.createStickerSet(createDto);

        // Then
        assertNotNull(result);
        assertEquals("shaitanchick", result.getName());

        verify(stickerSetRepository).findByName("shaitanchick");
        verify(telegramBotApiService).validateStickerSetExists("shaitanchick");
    }

    private Object createMockTelegramStickerSetInfo(String title) {
        // Создаем простой мок объекта с информацией о стикерсете
        return new Object() {
            @Override
            public String toString() {
                return "{\"title\":\"" + title + "\"}";
            }
        };
    }

    private StickerSet createMockSavedStickerSet(String name, String title, Long userId) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setId(1L);
        stickerSet.setName(name);
        stickerSet.setTitle(title);
        stickerSet.setUserId(userId);
        return stickerSet;
    }
}

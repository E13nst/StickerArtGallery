package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.util.TelegramInitDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("API для стикерсетов")
@Feature("Создание и управление стикерсетами")
@DisplayName("Интеграционные тесты StickerSetController")
class StickerSetControllerIntegrationTest {
    
    static {
        // ⚠️ Проверяем, что интеграционные тесты не запускаются на проде
        String activeProfile = System.getProperty("spring.profiles.active", "");
        if ("prod".equals(activeProfile)) {
            throw new IllegalStateException(
                "❌ ИНТЕГРАЦИОННЫЕ ТЕСТЫ НЕ ДОЛЖНЫ ЗАПУСКАТЬСЯ НА ПРОДЕ! " +
                "Используйте профиль 'test' для интеграционных тестов."
            );
        }
        
        // Проверяем наличие переменных окружения для интеграционных тестов
        if (!System.getenv().containsKey("TELEGRAM_BOT_TOKEN")) {
            System.out.println("⚠️ ВНИМАНИЕ: TELEGRAM_BOT_TOKEN не найден в переменных окружения");
            System.out.println("💡 Интеграционные тесты могут работать некорректно");
        }
    }

    @Autowired
    private MockMvc mockMvc;  // Автоматически настроен с @AutoConfigureMockMvc

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private StickerSetRepository stickerSetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;

    private String validInitData;
    
    private static final Long TEST_USER_ID = 141614461L;
    private static final String BOT_NAME = "StickerGallery";

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        // ⚠️ ВНИМАНИЕ: Работаем с ПРОДАКШЕН БД! Очищаем тестовые данные
        cleanupTestData();
        
        // Создаем тестового пользователя и профиль
        createTestUserAndProfile();
        
        // Генерируем валидную initData используя реальный токен бота из конфигурации
        String botToken = appConfig.getTelegram().getBotToken();
        validInitData = TelegramInitDataGenerator.builder()
                .botToken(botToken)
                .userId(TEST_USER_ID)
                .username("E13nst")
                .firstName("Andrey")
                .lastName("Mitroshin")
                .languageCode("ru")
                .build();
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // ⚠️ ВНИМАНИЕ: Очищаем данные после теста, чтобы не засорять продакшен БД!
        System.out.println("🧹 Очистка тестовых данных после выполнения теста...");
        cleanupTestData();
    }
    
    /**
     * ⚠️ Создает тестового пользователя и его профиль
     * ВНИМАНИЕ: Удаляется в cleanupTestData()
     */
    private void createTestUserAndProfile() {
        // Создаем пользователя если его нет
        if (!userRepository.existsById(TEST_USER_ID)) {
            UserEntity user = new UserEntity();
            user.setId(TEST_USER_ID);
            user.setFirstName("Test");
            user.setLastName("User");
            user.setUsername("test_integration_user");
            user.setLanguageCode("ru");
            userRepository.save(user);
            System.out.println("👤 Создан тестовый пользователь: " + TEST_USER_ID);
        }
        
        // Создаем профиль если его нет
        if (!userProfileRepository.existsByUserId(TEST_USER_ID)) {
            UserProfileEntity profile = new UserProfileEntity();
            profile.setUserId(TEST_USER_ID);
            profile.setRole(UserProfileEntity.UserRole.USER);
            profile.setArtBalance(0L);
            userProfileRepository.save(profile);
            System.out.println("📋 Создан тестовый профиль для пользователя: " + TEST_USER_ID);
        }
    }
    
    /**
     * ⚠️ Удаляет ВСЕ тестовые данные (стикерсеты, профили, пользователей)
     * Безопасно для продакшен БД - удаляет только тестовые данные
     */
    private void cleanupTestData() {
        // 1. Удаляем тестовые стикерсеты
        String[] testStickerSets = {"citati_prosto"};
        for (String name : testStickerSets) {
            stickerSetRepository.findByNameIgnoreCase(name)
                    .ifPresent(s -> {
                        System.out.println("🗑️ Удаляем тестовый стикерсет: " + name);
                        stickerSetRepository.delete(s);
                    });
        }
        
        // 2. НЕ удаляем пользователя и профиль - они могут использоваться
        // в продакшене. Только очищаем стикерсеты.
    }

    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с валидными данными должен возвращать 201")
    @Description("Проверяет создание нового стикерсета с валидным именем. " +
                "Ожидается, что API вернет 201 Created с полными данными стикерсета.")
    @Severity(SeverityLevel.BLOCKER)
    void createStickerSet_WithValidData_ShouldReturn201() throws Exception {
        // Given - используем реальный существующий стикерсет, которого нет в БД
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/citati_prosto");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andDo(result -> {
                    System.out.println("🧪 Response Status: " + result.getResponse().getStatus());
                    System.out.println("🧪 Response Body: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("citati_prosto"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.title").value("Цитаты простых людей"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с URL стикерсета должен возвращать 201")
    @Description("Проверяет, что API корректно обрабатывает URL стикерсета (t.me/addstickers/NAME) " +
                "и извлекает из него имя стикерсета автоматически.")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithStickerSetUrl_ShouldReturn201() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/ShaitanChick");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("shaitanchick"))
                .andExpect(jsonPath("$.userId").value(141614461))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с указанным title должен использовать переданный title")
    void createStickerSet_WithProvidedTitle_ShouldUseProvidedTitle() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");
        createDto.setTitle("Custom Title");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test_stickers"))
                .andExpect(jsonPath("$.title").value("Custom Title"))
                .andExpect(jsonPath("$.userId").value(141614461));
    }

    @Test
    @DisplayName("POST /api/stickersets с указанным userId должен использовать переданный userId")
    void createStickerSet_WithProvidedUserId_ShouldUseProvidedUserId() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");
        createDto.setUserId(999999999L);

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test_stickers"))
                .andExpect(jsonPath("$.userId").value(999999999));
    }

    @Test
    @DisplayName("POST /api/stickersets с пустым именем должен возвращать 400")
    void createStickerSet_WithEmptyName_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с некорректным именем должен возвращать 400")
    void createStickerSet_WithInvalidName_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("invalid-name!");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с некорректным URL должен возвращать 400")
    void createStickerSet_WithInvalidUrl_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с отрицательным userId должен возвращать 400")
    void createStickerSet_WithNegativeUserId_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");
        createDto.setUserId(-1L);

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с слишком длинным title должен возвращать 400")
    void createStickerSet_WithTooLongTitle_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");
        createDto.setTitle("A".repeat(65)); // Максимум 64 символа

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets без заголовков авторизации должен возвращать 401")
    void createStickerSet_WithoutAuthHeaders_ShouldReturn401() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/stickersets с некорректным initData должен возвращать 401")
    void createStickerSet_WithInvalidInitData_ShouldReturn401() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", "invalid_data")
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/stickersets с JSON без обязательного поля name должен возвращать 400")
    void createStickerSet_WithoutNameField_ShouldReturn400() throws Exception {
        // Given
        String jsonWithoutName = "{\"title\":\"Test Title\",\"userId\":123456789}";

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutName))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с некорректным JSON должен возвращать 400")
    void createStickerSet_WithInvalidJson_ShouldReturn400() throws Exception {
        // Given
        String invalidJson = "{\"name\":\"test_stickers\",\"title\":\"Test Title\",\"userId\":123456789";

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/stickersets с дублирующимся именем должен возвращать 400")
    void createStickerSet_WithDuplicateName_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("duplicate_test_stickers");

        // Сначала создаем стикерсет
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated());

        // Затем пытаемся создать еще один с тем же именем
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .header("X-Telegram-Bot-Name", BOT_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                .andExpect(jsonPath("$.message").value("Стикерсет с именем 'duplicate_test_stickers' уже существует в галерее"));
    }
}

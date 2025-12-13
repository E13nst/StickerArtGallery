package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.sticker_art_gallery.util.TelegramInitDataGenerator;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@org.springframework.test.context.TestPropertySource(properties = "app.internal.service-tokens.sticker-bot=test-internal-token")
@Epic("API для стикерсетов")
@Feature("Создание и управление стикерсетами")
@DisplayName("Интеграционные тесты StickerSetController")
@Tag("integration")  // Запускаются только явно: make test-integration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    private StickerSetTestSteps testSteps;
    private String validInitData;
    
    private static final Long TEST_USER_ID = TestDataBuilder.TEST_USER_ID;

    @BeforeAll
    void setUp() throws Exception {
        // Инициализируем testSteps один раз для всех тестов
        testSteps = new StickerSetTestSteps();
        testSteps.setMockMvc(mockMvc);
        testSteps.setObjectMapper(objectMapper);
        testSteps.setAppConfig(appConfig);
        testSteps.setStickerSetRepository(stickerSetRepository);
        testSteps.setUserRepository(userRepository);
        testSteps.setUserProfileRepository(userProfileRepository);
        
        // ⚠️ ВНИМАНИЕ: Работаем с ПРОДАКШЕН БД! Очищаем тестовые данные
        testSteps.cleanupTestData();
        
        // Создаем тестового пользователя и профиль один раз для всех тестов
        testSteps.createTestUserAndProfile(TEST_USER_ID);
        
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
    
    @AfterAll
    void tearDown() {
        // ⚠️ ВНИМАНИЕ: Очищаем данные после всех тестов, чтобы не засорять продакшен БД!
        System.out.println("🧹 Очистка тестовых данных после выполнения всех тестов...");
        testSteps.cleanupTestData();
    }
    
    @AfterEach
    void cleanupAfterTest() {
        // Очищаем тестовые стикерсеты после каждого теста
        // (так как тесты создают стикерсеты через API)
        testSteps.cleanupTestData();
    }

    private org.springframework.test.web.servlet.ResultActions performCreateStickerSet(CreateStickerSetDto createDto, String initData) throws Exception {
        return mockMvc.perform(post("/api/stickersets")
                .header("X-Telegram-Init-Data", initData)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)));
    }

    private org.springframework.test.web.servlet.ResultActions performInternalCreateStickerSet(String serviceToken,
                                                                                               CreateStickerSetDto createDto,
                                                                                               Long userId,
                                                                                               String language) throws Exception {
        var requestBuilder = post("/internal/stickersets")
                .param("userId", String.valueOf(userId));
        if (language != null) {
            requestBuilder = requestBuilder.param("language", language);
        }
        if (serviceToken != null) {
            requestBuilder = requestBuilder.header("X-Service-Token", serviceToken);
        }
        return mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)));
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
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
        performCreateStickerSet(createDto, validInitData)
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
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с URL стикерсета должен возвращать 201")
    @Description("Проверяет, что API корректно обрабатывает URL стикерсета (t.me/addstickers/NAME) " +
                "и извлекает из него имя стикерсета автоматически.")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithStickerSetUrl_ShouldReturn201() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/shblokun");

        // When & Then
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("shblokun"))
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
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test_stickers"))
                .andExpect(jsonPath("$.title").value("Custom Title"))
                .andExpect(jsonPath("$.userId").value(141614461));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "invalid-name",
            "name with spaces",
            "Name@With#Special$Chars",
            "name.with.dots",
            "name,with,commas",
            "name!with!exclamation",
            "name?with?question",
            "name(with)parentheses",
            "https://t.me/addstickers/",
            "https://t.me/addstickers/invalid-name",
            "https://t.me/addstickers/name with spaces",
            "ftp://t.me/addstickers/Test",
            "http://example.com/addstickers/Test"
    })
    @DisplayName("POST /api/stickersets: валидация некорректных имен и URL -> 400 Bad Request")
    @Tag("validation")
    void createStickerSet_WithInvalidNames_ShouldReturn400(String invalidName) throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName(invalidName);

        // When & Then
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @ValueSource(ints = {65, 100, 200})
    @DisplayName("POST /api/stickersets: валидация слишком длинного title -> 400 Bad Request")
    @Tag("validation")
    void createStickerSet_WithTooLongTitle_ShouldReturn400(int titleLength) throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");
        createDto.setTitle("A".repeat(titleLength)); // Максимум 64 символа

        // When & Then
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets без заголовков авторизации должен возвращать 400")
    void createStickerSet_WithoutAuthHeaders_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("User is not authenticated"));
    }

    @Test
    @DisplayName("POST /api/stickersets с некорректным initData должен возвращать 401")
    void createStickerSet_WithInvalidInitData_ShouldReturn401() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_stickers");

        // When & Then
        performCreateStickerSet(createDto, "invalid_data")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("User is not authenticated"));
    }

    @Test
    @DisplayName("POST /internal/stickersets с валидным токеном должен возвращать 201")
    void createStickerSet_InternalEndpoint_WithValidToken_ShouldReturn201() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/shblokun");
        createDto.setIsPublic(false);

        // When & Then
        performInternalCreateStickerSet("test-internal-token", createDto, TEST_USER_ID, "ru")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.name").value("shblokun"))
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    @DisplayName("POST /internal/stickersets без токена должен возвращать 401")
    void createStickerSet_InternalEndpoint_WithoutToken_ShouldReturn401() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/citati_prosto");

        // When & Then
        performInternalCreateStickerSet(null, createDto, TEST_USER_ID, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Missing service token"));
    }

    @Test
    @DisplayName("POST /api/stickersets с JSON без обязательного поля name должен возвращать 400")
    void createStickerSet_WithoutNameField_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();

        // When & Then
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/stickersets с некорректным JSON должен возвращать 400")
    void createStickerSet_WithInvalidJson_ShouldReturn400() throws Exception {
        // Given
        String invalidJson = "{\"name\":\"test_stickers\",\"title\":\"Test Title\"";

        // When & Then
        mockMvc.perform(post("/api/stickersets")
                        .header("X-Telegram-Init-Data", validInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/stickersets с дублирующимся именем должен возвращать 400")
    void createStickerSet_WithDuplicateName_ShouldReturn400() throws Exception {
        // Given - используем существующий стикерсет
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("shblokun");

        // Сначала создаем стикерсет
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isCreated());

        // Затем пытаемся создать еще один с тем же именем
        performCreateStickerSet(createDto, validInitData)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                .andExpect(jsonPath("$.message").value("Стикерсет с именем 'shblokun' уже существует в галерее"));
    }
}

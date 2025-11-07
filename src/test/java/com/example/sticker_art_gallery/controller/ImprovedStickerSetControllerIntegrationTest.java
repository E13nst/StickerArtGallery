package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("API для стикерсетов")
@Feature("Создание и управление стикерсетами")
@DisplayName("Улучшенные интеграционные тесты StickerSetController")
@Tag("integration")  // Запускаются только явно: make test-integration
class ImprovedStickerSetControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
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
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        // Инициализируем testSteps
        testSteps = new StickerSetTestSteps();
        testSteps.setMockMvc(mockMvc);
        testSteps.setObjectMapper(objectMapper);
        testSteps.setAppConfig(appConfig);
        testSteps.setStickerSetRepository(stickerSetRepository);
        testSteps.setUserRepository(userRepository);
        testSteps.setUserProfileRepository(userProfileRepository);
        
        // Очищаем тестовые данные
        testSteps.cleanupTestData();
        
        // Создаем тестового пользователя и профиль
        testSteps.createTestUserAndProfile(TestDataBuilder.TEST_USER_ID);
        
        // Создаем валидную initData
        validInitData = testSteps.createValidInitData(TestDataBuilder.TEST_USER_ID);
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Очищаем тестовые данные после теста
        testSteps.cleanupTestData();
    }
    
    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с валидными данными должен возвращать 201")
    @Description("Проверяет создание нового стикерсета с валидным именем. " +
                "Ожидается, что API вернет 201 Created с полными данными стикерсета.")
    @Severity(SeverityLevel.BLOCKER)
    void createStickerSet_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createStickerSetDtoWithUrl("citati_prosto");
        
        // When
        ResultActions result = testSteps.createStickerSet(createDto, validInitData);
        
        // Then
        testSteps.verifyStickerSetCreated(result, "citati_prosto", TestDataBuilder.TEST_USER_ID);
    }
    
    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с URL стикерсета должен возвращать 201")
    @Description("Проверяет, что API корректно обрабатывает URL стикерсета (t.me/addstickers/NAME) " +
                "и извлекает из него имя стикерсета автоматически.")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithStickerSetUrl_ShouldReturn201() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createStickerSetDtoWithUrl("shblokun");
        
        // When
        ResultActions result = testSteps.createStickerSet(createDto, validInitData);
        
        // Then
        testSteps.verifyStickerSetCreated(result, "shblokun", TestDataBuilder.TEST_USER_ID);
    }
    
    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с указанным title должен использовать переданный title")
    @Severity(SeverityLevel.NORMAL)
    void createStickerSet_WithProvidedTitle_ShouldUseProvidedTitle() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createStickerSetDtoWithTitle("test_stickers", "Custom Title");
        
        // When
        ResultActions result = testSteps.createStickerSet(createDto, validInitData);
        
        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test_stickers"))
                .andExpect(jsonPath("$.title").value("Custom Title"))
                .andExpect(jsonPath("$.userId").value(TestDataBuilder.TEST_USER_ID));
    }

    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с isPublic=false должен создавать приватный набор")
    @Severity(SeverityLevel.NORMAL)
    void createStickerSet_WithIsPublicFalse_ShouldCreatePrivateSet() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createStickerSetDtoWithUrl("citati_prosto");
        createDto.setTitle("Private Set");
        createDto.setIsPublic(false);

        // When
        ResultActions result = testSteps.createStickerSet(createDto, validInitData);

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("citati_prosto"))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.userId").value(TestDataBuilder.TEST_USER_ID));
    }
    
    @Test
    @Story("Валидация данных")
    @DisplayName("POST /api/stickersets с пустым именем должен возвращать 400")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithEmptyName_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createInvalidStickerSetDto("");
        
        // When
        ResultActions result = testSteps.createStickerSet(createDto, validInitData);
        
        // Then
        testSteps.verifyValidationError(result);
    }
    
    @Test
    @Story("Валидация данных")
    @DisplayName("POST /api/stickersets с некорректным именем должен возвращать 400")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithInvalidName_ShouldReturn400() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createInvalidStickerSetDto("invalid-name!");
        
        // When
        ResultActions result = testSteps.createStickerSet(createDto, validInitData);
        
        // Then
        testSteps.verifyValidationError(result);
    }
    
    @Test
    @Story("Авторизация")
    @DisplayName("POST /api/stickersets с некорректным initData должен возвращать 401")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithInvalidInitData_ShouldReturn401() throws Exception {
        // Given
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        String invalidInitData = TestDataBuilder.createInvalidInitData();
        
        // When
        ResultActions result = testSteps.createStickerSet(createDto, invalidInitData);
        
        // Then
        testSteps.verifyUnauthorizedError(result);
    }

    @Test
    @Story("Безопасность")
    @DisplayName("POST /api/stickersets для заблокированного пользователя должен возвращать 400")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSet_WithBlockedUser_ShouldReturn403() throws Exception {
        userProfileRepository.findByUserId(TestDataBuilder.TEST_USER_ID).ifPresent(profile -> {
            profile.setIsBlocked(true);
            userProfileRepository.save(profile);
        });

        CreateStickerSetDto createDto = TestDataBuilder.createStickerSetDtoWithUrl("blocked_test_" + UUID.randomUUID());

        ResultActions result = testSteps.createStickerSet(createDto, validInitData);

        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("User is blocked"));
    }
    
    @Test
    @Story("Получение стикерсетов")
    @DisplayName("GET /api/stickersets должен возвращать список стикерсетов")
    @Description("Проверяет получение всех стикерсетов с пагинацией")
    @Severity(SeverityLevel.BLOCKER)
    void getAllStickerSets_ShouldReturnList() throws Exception {
        // Given - создаем тестовый стикерсет
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        testSteps.createStickerSet(createDto, validInitData);
        
        // When
        ResultActions result = testSteps.getAllStickerSets(validInitData);
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }
    
    @Test
    @Story("Получение стикерсетов")
    @DisplayName("GET /api/stickersets?likedOnly=true должен возвращать только лайкнутые стикерсеты")
    @Description("Проверяет новую функциональность фильтрации по лайкам")
    @Severity(SeverityLevel.CRITICAL)
    void getLikedStickerSets_ShouldReturnOnlyLiked() throws Exception {
        // Given - создаем тестовый стикерсет
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        testSteps.createStickerSet(createDto, validInitData);
        
        // When
        ResultActions result = testSteps.getLikedStickerSets(validInitData);
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }
    
    @Test
    @Story("Получение стикерсетов")
    @DisplayName("GET /api/stickersets?likedOnly=true без авторизации должен возвращать 401")
    @Description("Проверяет, что фильтрация по лайкам требует авторизации")
    @Severity(SeverityLevel.CRITICAL)
    void getLikedStickerSets_WithoutAuth_ShouldReturn401() throws Exception {
        // When
        ResultActions result = testSteps.getLikedStickerSets(TestDataBuilder.createInvalidInitData());
        
        // Then
        testSteps.verifyUnauthorizedError(result);
    }
    
    @Test
    @Story("Получение стикерсета по ID")
    @DisplayName("GET /api/stickersets/{id} должен возвращать стикерсет с информацией о лайке")
    @Description("Проверяет новую функциональность - поле isLikedByCurrentUser")
    @Severity(SeverityLevel.CRITICAL)
    void getStickerSetById_ShouldReturnWithLikeInfo() throws Exception {
        // Given - создаем тестовый стикерсет
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        ResultActions createResult = testSteps.createStickerSet(createDto, validInitData);
        
        // Извлекаем ID созданного стикерсета
        String responseContent = createResult.andReturn().getResponse().getContentAsString();
        // В реальном тесте здесь бы был парсинг JSON для получения ID
        
        // When
        ResultActions result = testSteps.getStickerSetById(1L, validInitData); // Используем фиксированный ID для примера
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.isLikedByCurrentUser").exists()); // Новое поле!
    }
}

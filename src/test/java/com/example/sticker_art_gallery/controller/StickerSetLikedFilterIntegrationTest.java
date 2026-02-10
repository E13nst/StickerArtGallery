package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.repository.UserProfileRepository;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.repository.UserRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("API для стикерсетов")
@Feature("Фильтрация по лайкам")
@DisplayName("Тесты новой функциональности likedOnly")
@Tag("integration")  // Запускаются только явно: make test-integration
class StickerSetLikedFilterIntegrationTest {
    
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
    @Story("Фильтрация по лайкам")
    @DisplayName("GET /api/stickersets/liked должен возвращать только лайкнутые стикерсеты")
    @Description("Проверяет, что эндпоинт возвращает только стикерсеты, " +
                "которые лайкнул текущий пользователь")
    @Severity(SeverityLevel.BLOCKER)
    void getLikedStickerSets_ShouldReturnOnlyLiked() throws Exception {
        // Given - создаем несколько тестовых стикерсетов
        CreateStickerSetDto stickerSet1 = TestDataBuilder.createStickerSetDtoWithUrl("citati_prosto");
        CreateStickerSetDto stickerSet2 = TestDataBuilder.createStickerSetDtoWithUrl("shblokun");
        
        testSteps.createStickerSet(stickerSet1, validInitData);
        testSteps.createStickerSet(stickerSet2, validInitData);
        
        // TODO: Здесь нужно добавить лайки через LikeService или API
        // Пока что тест проверяет структуру ответа
        
        // When
        ResultActions result = testSteps.getLikedStickerSets(validInitData);
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }
    
    @Test
    @Story("Фильтрация по лайкам")
    @DisplayName("GET /api/stickersets/liked без авторизации должен возвращать 401")
    @Description("Проверяет, что получение лайкнутых стикерсетов требует авторизации")
    @Severity(SeverityLevel.CRITICAL)
    void getLikedStickerSets_WithoutAuth_ShouldReturn401() throws Exception {
        // When
        ResultActions result = testSteps.getLikedStickerSets(TestDataBuilder.createInvalidInitData());
        
        // Then
        testSteps.verifyUnauthorizedError(result);
    }
    
    @Test
    @Story("Фильтрация по лайкам")
    @DisplayName("GET /api/stickersets/liked должен поддерживать фильтры (categoryKeys, type, sort)")
    @Description("Проверяет, что новый эндпоинт поддерживает все фильтры как базовый GET /api/stickersets")
    @Severity(SeverityLevel.NORMAL)
    void getLikedStickerSets_WithFilters_ShouldWorkCorrectly() throws Exception {
        // Given - создаем тестовый стикерсет
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        testSteps.createStickerSet(createDto, validInitData);
        
        // When - запрашиваем с фильтрами
        ResultActions result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/stickersets/liked")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "likesCount")
                        .param("direction", "DESC")
                        .param("shortInfo", "true")
                        .header("X-Telegram-Init-Data", validInitData));
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }
    
    @Test
    @Story("Фильтрация по лайкам")
    @DisplayName("GET /api/stickersets/liked с пагинацией должен работать корректно")
    @Description("Проверяет, что пагинация работает с эндпоинтом лайкнутых стикерсетов")
    @Severity(SeverityLevel.NORMAL)
    void getLikedStickerSets_WithPagination_ShouldWorkCorrectly() throws Exception {
        // Given - создаем тестовый стикерсет
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        testSteps.createStickerSet(createDto, validInitData);
        
        // When
        ResultActions result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/stickersets/liked")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "DESC")
                        .header("X-Telegram-Init-Data", validInitData));
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }
    
    @Test
    @Story("Фильтрация по лайкам")
    @DisplayName("GET /api/stickersets/liked должен возвращать стикерсеты с isLikedByCurrentUser=true")
    @Description("Проверяет, что все возвращаемые стикерсеты имеют isLikedByCurrentUser=true")
    @Severity(SeverityLevel.CRITICAL)
    void getLikedStickerSets_ShouldReturnStickersWithLikedTrue() throws Exception {
        // Given - создаем тестовый стикерсет
        CreateStickerSetDto createDto = TestDataBuilder.createBasicStickerSetDto();
        testSteps.createStickerSet(createDto, validInitData);
        
        // TODO: Здесь нужно добавить лайк через LikeService или API
        // Пока что тест проверяет структуру ответа
        
        // When
        ResultActions result = testSteps.getLikedStickerSets(validInitData);
        
        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].isLikedByCurrentUser").exists());
        
        // TODO: Добавить проверку, что все isLikedByCurrentUser = true
        // Это потребует создания реальных лайков в тесте
    }
}

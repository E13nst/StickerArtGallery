package com.example.sticker_art_gallery.teststeps;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * Общие шаги для интеграционных тестов
 * Выделяет повторяющуюся логику в отдельные методы с @Step аннотациями
 */
@Component
public class StickerSetTestSteps {
    
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
    
    // Setter методы для ручной инициализации
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
    
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    public void setStickerSetRepository(StickerSetRepository stickerSetRepository) {
        this.stickerSetRepository = stickerSetRepository;
    }
    
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public void setUserProfileRepository(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }
    
    @Step("Создать тестового пользователя и профиль")
    public void createTestUserAndProfile(Long userId) {
        // Создаем пользователя если его нет
        if (!userRepository.existsById(userId)) {
            UserEntity user = TestDataBuilder.createTestUser(userId);
            userRepository.save(user);
            System.out.println("👤 Создан тестовый пользователь: " + userId);
        }
        
        // Создаем профиль если его нет
        if (!userProfileRepository.existsByUserId(userId)) {
            UserProfileEntity profile = TestDataBuilder.createTestUserProfile(userId);
            userProfileRepository.save(profile);
            System.out.println("📋 Создан тестовый профиль для пользователя: " + userId);
        }
    }

    @Step("Назначить пользователю роль ADMIN")
    public void makeAdmin(Long userId) {
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfileEntity p = TestDataBuilder.createTestUserProfile(userId);
                    return userProfileRepository.save(p);
                });
        profile.setRole(UserProfileEntity.UserRole.ADMIN);
        userProfileRepository.save(profile);
    }
    
    @Step("Очистить тестовые данные")
    public void cleanupTestData() {
        // Удаляем тестовые стикерсеты - используем findAll() вместо findByNameIgnoreCase()
        for (String name : TestDataBuilder.TEST_STICKER_SETS) {
            List<com.example.sticker_art_gallery.model.telegram.StickerSet> stickerSets = 
                stickerSetRepository.findAll().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name))
                    .collect(java.util.stream.Collectors.toList());
            
            for (com.example.sticker_art_gallery.model.telegram.StickerSet s : stickerSets) {
                System.out.println("🗑️ Удаляем тестовый стикерсет: " + name + " (ID: " + s.getId() + ")");
                stickerSetRepository.delete(s);
            }
        }
    }
    
    @Step("Создать стикерсет через API")
    public ResultActions createStickerSet(CreateStickerSetDto createDto, String initData) throws Exception {
        return mockMvc.perform(post("/api/stickersets")
                .header("X-Telegram-Init-Data", initData)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)));
    }
    
    @Step("Получить все стикерсеты через API")
    public ResultActions getAllStickerSets(String initData) throws Exception {
        return getAllStickerSets(initData, null);
    }
    
    @Step("Получить все стикерсеты через API с параметрами")
    public ResultActions getAllStickerSets(String initData, java.util.Map<String, String> queryParams) throws Exception {
        var requestBuilder = get("/api/stickersets")
                .header("X-Telegram-Init-Data", initData);
        if (queryParams != null) {
            for (java.util.Map.Entry<String, String> entry : queryParams.entrySet()) {
                requestBuilder = requestBuilder.param(entry.getKey(), entry.getValue());
            }
        }
        return mockMvc.perform(requestBuilder);
    }
    
    @Step("Получить только лайкнутые стикерсеты через API")
    public ResultActions getLikedStickerSets(String initData) throws Exception {
        return mockMvc.perform(get("/api/stickersets")
                        .param("likedOnly", "true")
                        .header("X-Telegram-Init-Data", initData));
    }

    @Step("Получить стикерсеты конкретного пользователя через API")
    public ResultActions getStickerSetsByUser(Long userId, String initData, java.util.Map<String, String> queryParams) throws Exception {
        var requestBuilder = get("/api/stickersets")
                .header("X-Telegram-Init-Data", initData)
                .param("userId", String.valueOf(userId));
        if (queryParams != null) {
            for (java.util.Map.Entry<String, String> entry : queryParams.entrySet()) {
                requestBuilder = requestBuilder.param(entry.getKey(), entry.getValue());
            }
        }
        return mockMvc.perform(requestBuilder);
    }

    public ResultActions getStickerSetsByUser(Long userId, String initData) throws Exception {
        return getStickerSetsByUser(userId, initData, null);
    }

    @Step("Получить стикерсеты по автору через API")
    public ResultActions getStickerSetsByAuthor(Long authorId, String initData, java.util.Map<String, String> queryParams) throws Exception {
        var requestBuilder = get("/api/stickersets")
                .header("X-Telegram-Init-Data", initData)
                .param("authorId", String.valueOf(authorId));
        if (queryParams != null) {
            for (java.util.Map.Entry<String, String> entry : queryParams.entrySet()) {
                requestBuilder = requestBuilder.param(entry.getKey(), entry.getValue());
            }
        }
        return mockMvc.perform(requestBuilder);
    }

    public ResultActions getStickerSetsByAuthor(Long authorId, String initData) throws Exception {
        return getStickerSetsByAuthor(authorId, initData, null);
    }
    
    @Step("Получить стикерсеты пользователя через новый эндпоинт /api/stickersets/user/{userId}")
    public ResultActions getUserStickerSets(Long userId, String initData, java.util.Map<String, String> queryParams) throws Exception {
        var requestBuilder = get("/api/stickersets/user/" + userId)
                .header("X-Telegram-Init-Data", initData);
        if (queryParams != null) {
            for (java.util.Map.Entry<String, String> entry : queryParams.entrySet()) {
                requestBuilder = requestBuilder.param(entry.getKey(), entry.getValue());
            }
        }
        return mockMvc.perform(requestBuilder);
    }
    
    public ResultActions getUserStickerSets(Long userId, String initData) throws Exception {
        return getUserStickerSets(userId, initData, null);
    }
    
    @Step("Получить стикерсеты автора через новый эндпоинт /api/stickersets/author/{authorId}")
    public ResultActions getAuthorStickerSets(Long authorId, String initData, java.util.Map<String, String> queryParams) throws Exception {
        var requestBuilder = get("/api/stickersets/author/" + authorId)
                .header("X-Telegram-Init-Data", initData);
        if (queryParams != null) {
            for (java.util.Map.Entry<String, String> entry : queryParams.entrySet()) {
                requestBuilder = requestBuilder.param(entry.getKey(), entry.getValue());
            }
        }
        return mockMvc.perform(requestBuilder);
    }
    
    public ResultActions getAuthorStickerSets(Long authorId, String initData) throws Exception {
        return getAuthorStickerSets(authorId, initData, null);
    }
    
    @Step("Получить стикерсет по ID через API")
    public ResultActions getStickerSetById(Long id, String initData) throws Exception {
        return mockMvc.perform(get("/api/stickersets/" + id)
                        .header("X-Telegram-Init-Data", initData));
    }
    
    @Step("Получить стикерсеты с фильтрацией по категориям")
    public ResultActions getStickerSetsByCategory(String categoryKeys, String initData) throws Exception {
        return mockMvc.perform(get("/api/stickersets")
                        .param("categoryKeys", categoryKeys)
                        .header("X-Telegram-Init-Data", initData));
    }

    @Step("Получить стикерсеты с фильтрами officialOnly/authorId/hasAuthorOnly")
    public ResultActions getStickerSetsWithFilters(Boolean officialOnly, Long authorId, Boolean hasAuthorOnly, String initData) throws Exception {
        return getStickerSetsWithFilters(officialOnly, authorId, hasAuthorOnly, initData, null);
    }
    
    public ResultActions getStickerSetsWithFilters(Boolean officialOnly, Long authorId, Boolean hasAuthorOnly, String initData, java.util.Map<String, String> additionalParams) throws Exception {
        var req = get("/api/stickersets")
                .header("X-Telegram-Init-Data", initData);
        if (officialOnly != null) req = req.param("officialOnly", officialOnly.toString());
        if (authorId != null) req = req.param("authorId", authorId.toString());
        if (hasAuthorOnly != null) req = req.param("hasAuthorOnly", hasAuthorOnly.toString());
        if (additionalParams != null) {
            for (java.util.Map.Entry<String, String> entry : additionalParams.entrySet()) {
                req = req.param(entry.getKey(), entry.getValue());
            }
        }
        return mockMvc.perform(req);
    }

    @Step("Получить топ по лайкам с фильтрами officialOnly/authorId/hasAuthorOnly")
    public ResultActions getTopByLikesWithFilters(Boolean officialOnly, Long authorId, Boolean hasAuthorOnly, String initData) throws Exception {
        var req = get("/api/stickersets")
                .header("X-Telegram-Init-Data", initData)
                .param("sort", "likesCount")
                .param("direction", "DESC");
        if (officialOnly != null) req = req.param("officialOnly", officialOnly.toString());
        if (authorId != null) req = req.param("authorId", authorId.toString());
        if (hasAuthorOnly != null) req = req.param("hasAuthorOnly", hasAuthorOnly.toString());
        return mockMvc.perform(req);
    }

    @Step("Отметить стикерсет как официальный (ADMIN)")
    public ResultActions markOfficial(Long id, String initData) throws Exception {
        return mockMvc.perform(put("/api/stickersets/" + id + "/official")
                .header("X-Telegram-Init-Data", initData));
    }

    @Step("Снять официальный статус (ADMIN)")
    public ResultActions markUnofficial(Long id, String initData) throws Exception {
        return mockMvc.perform(put("/api/stickersets/" + id + "/unofficial")
                .header("X-Telegram-Init-Data", initData));
    }

    @Step("Установить автора (ADMIN)")
    public ResultActions setAuthor(Long id, Long authorId, String initData) throws Exception {
        java.util.Map<String, Long> body = java.util.Map.of("authorId", authorId);
        return mockMvc.perform(put("/api/stickersets/" + id + "/author")
                .header("X-Telegram-Init-Data", initData)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Step("Очистить автора (ADMIN)")
    public ResultActions clearAuthor(Long id, String initData) throws Exception {
        return mockMvc.perform(delete("/api/stickersets/" + id + "/author")
                .header("X-Telegram-Init-Data", initData));
    }

    @Step("Опубликовать стикерсет")
    public ResultActions publishStickerSet(Long id, String initData) throws Exception {
        return mockMvc.perform(post("/api/stickersets/" + id + "/publish")
                .header("X-Telegram-Init-Data", initData));
    }

    @Step("Скрыть стикерсет из галереи")
    public ResultActions unpublishStickerSet(Long id, String initData) throws Exception {
        return mockMvc.perform(post("/api/stickersets/" + id + "/unpublish")
                .header("X-Telegram-Init-Data", initData));
    }

    @Step("Заблокировать стикерсет (ADMIN)")
    public ResultActions blockStickerSet(Long id, String initData) throws Exception {
        return blockStickerSet(id, null, initData);
    }

    @Step("Заблокировать стикерсет с причиной (ADMIN)")
    public ResultActions blockStickerSet(Long id, String reason, String initData) throws Exception {
        java.util.Map<String, String> body = reason != null ? java.util.Map.of("reason", reason) : null;
        var requestBuilder = put("/api/stickersets/" + id + "/block")
                .header("X-Telegram-Init-Data", initData);
        if (body != null) {
            requestBuilder = requestBuilder
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(requestBuilder);
    }

    @Step("Разблокировать стикерсет (ADMIN)")
    public ResultActions unblockStickerSet(Long id, String initData) throws Exception {
        return mockMvc.perform(put("/api/stickersets/" + id + "/unblock")
                .header("X-Telegram-Init-Data", initData));
    }
    
    @Step("Создать валидную initData")
    public String createValidInitData(Long userId) {
        String botToken = appConfig.getTelegram().getBotToken();
        return TestDataBuilder.createValidInitData(botToken, userId);
    }
    
    @Step("Проверить успешное создание стикерсета")
    public void verifyStickerSetCreated(ResultActions result, String expectedName, Long expectedUserId) throws Exception {
        result.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name").value(expectedName))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.userId").value(expectedUserId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.isPublic").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.createdAt").exists());
    }
    
    @Step("Проверить ошибку валидации")
    public void verifyValidationError(ResultActions result) throws Exception {
        result.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").exists());
    }
    
    @Step("Проверить ошибку авторизации")
    public void verifyUnauthorizedError(ResultActions result) throws Exception {
        result.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }
}

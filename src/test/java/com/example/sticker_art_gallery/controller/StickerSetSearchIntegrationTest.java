package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.util.TelegramInitDataGenerator;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.test.context.TestPropertySource(properties = "app.internal.service-tokens.sticker-bot=test-internal-token")
@Epic("API для стикерсетов")
@Feature("Поиск стикерсетов")
@DisplayName("Интеграционные тесты поиска стикерсетов")
@Tag("integration")
class StickerSetSearchIntegrationTest {
    
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
    
    private String validInitData;
    private static final Long TEST_USER_ID = 141614461L;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        cleanupTestData();
        createTestUserAndProfile();
        
        String botToken = appConfig.getTelegram().getBotToken();
        validInitData = TelegramInitDataGenerator.builder()
                .botToken(botToken)
                .userId(TEST_USER_ID)
                .username("TestUser")
                .firstName("Test")
                .lastName("User")
                .languageCode("en")
                .build();
        
        // Создаем тестовые стикерсеты для поиска
        createTestStickerSets();
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanupTestData();
    }
    
    private void createTestStickerSets() {
        // Стикерсет с "cat" в title
        StickerSet catStickers = new StickerSet();
        catStickers.setUserId(TEST_USER_ID);
        catStickers.setTitle("Funny Cat Stickers");
        catStickers.setDescription("Collection of cute cats");
        catStickers.setName("funny_cat_stickers_test");
        catStickers.setState(StickerSetState.ACTIVE);
        catStickers.setVisibility(StickerSetVisibility.PUBLIC);
        catStickers.setType(StickerSetType.USER);
        stickerSetRepository.save(catStickers);
        
        // Стикерсет с "dog" в title
        StickerSet dogStickers = new StickerSet();
        dogStickers.setUserId(TEST_USER_ID);
        dogStickers.setTitle("Happy Dogs Pack");
        dogStickers.setDescription("Best dog stickers ever");
        dogStickers.setName("happy_dogs_pack_test");
        dogStickers.setState(StickerSetState.ACTIVE);
        dogStickers.setVisibility(StickerSetVisibility.PUBLIC);
        dogStickers.setType(StickerSetType.USER);
        stickerSetRepository.save(dogStickers);
        
        // Стикерсет с "cat" в description
        StickerSet animalStickers = new StickerSet();
        animalStickers.setUserId(TEST_USER_ID);
        animalStickers.setTitle("Animal Kingdom");
        animalStickers.setDescription("Features cute cat and dog images");
        animalStickers.setName("animal_kingdom_test");
        animalStickers.setState(StickerSetState.ACTIVE);
        animalStickers.setVisibility(StickerSetVisibility.PUBLIC);
        animalStickers.setType(StickerSetType.USER);
        stickerSetRepository.save(animalStickers);
        
        // Приватный стикерсет (не должен находиться)
        StickerSet privateStickers = new StickerSet();
        privateStickers.setUserId(TEST_USER_ID);
        privateStickers.setTitle("Private Cat Collection");
        privateStickers.setDescription("My personal cat stickers");
        privateStickers.setName("private_cat_test");
        privateStickers.setState(StickerSetState.ACTIVE);
        privateStickers.setVisibility(StickerSetVisibility.PRIVATE);
        privateStickers.setType(StickerSetType.USER);
        stickerSetRepository.save(privateStickers);
        
        // Заблокированный стикерсет (не должен находиться)
        StickerSet blockedStickers = new StickerSet();
        blockedStickers.setUserId(TEST_USER_ID);
        blockedStickers.setTitle("Blocked Cat Stickers");
        blockedStickers.setDescription("These cats are blocked");
        blockedStickers.setName("blocked_cat_test");
        blockedStickers.setState(StickerSetState.BLOCKED);
        blockedStickers.setVisibility(StickerSetVisibility.PUBLIC);
        blockedStickers.setType(StickerSetType.USER);
        stickerSetRepository.save(blockedStickers);
        
        // Официальный стикерсет
        StickerSet officialStickers = new StickerSet();
        officialStickers.setUserId(TEST_USER_ID);
        officialStickers.setTitle("Official Cat Pack");
        officialStickers.setDescription("Official Telegram cats");
        officialStickers.setName("official_cat_test");
        officialStickers.setState(StickerSetState.ACTIVE);
        officialStickers.setVisibility(StickerSetVisibility.PUBLIC);
        officialStickers.setType(StickerSetType.OFFICIAL);
        stickerSetRepository.save(officialStickers);
    }
    
    @Test
    @Story("Поиск по title")
    @DisplayName("Поиск стикерсетов по совпадению в title")
    @Description("Должен найти все публичные активные стикерсеты с 'cat' в названии")
    void testSearchByTitle() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "cat")
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2)))) // Минимум 2: Funny Cat и Official Cat
                .andExpect(jsonPath("$.content[*].title", hasItem(containsStringIgnoringCase("cat"))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
    }
    
    @Test
    @Story("Поиск по description")
    @DisplayName("Поиск стикерсетов по совпадению в description")
    @Description("Должен найти стикерсеты с 'dog' в описании")
    void testSearchByDescription() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "dog")
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1)))) // Минимум Happy Dogs или Animal Kingdom
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }
    
    @Test
    @Story("Фильтрация по типу")
    @DisplayName("Поиск только официальных стикерсетов")
    @Description("Должен найти только официальные стикерсеты с 'cat'")
    void testSearchWithTypeFilter() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "cat")
                .param("type", "OFFICIAL")
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1)))) // Минимум 1 (наш Official Cat Pack)
                .andExpect(jsonPath("$.content[*].type", everyItem(is("OFFICIAL")))) // Все должны быть OFFICIAL
                .andExpect(jsonPath("$.content[*].title", hasItem(containsStringIgnoringCase("cat")))); // Хотя бы один с "cat"
    }
    
    @Test
    @Story("Исключение приватных и заблокированных")
    @DisplayName("Поиск не возвращает приватные и заблокированные стикерсеты")
    @Description("Приватные и заблокированные стикерсеты не должны появляться в результатах")
    void testSearchExcludesPrivateAndBlocked() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "cat")
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Private Cat Collection"))))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Blocked Cat Stickers"))))
                .andExpect(jsonPath("$.content[*].visibility", everyItem(is("PUBLIC"))))
                .andExpect(jsonPath("$.content[*].state", everyItem(is("ACTIVE"))));
    }
    
    @Test
    @Story("Регистронезависимый поиск")
    @DisplayName("Поиск не зависит от регистра")
    @Description("Должен найти результаты независимо от регистра запроса")
    void testSearchCaseInsensitive() throws Exception {
        // Тестируем разные варианты регистра
        for (String query : new String[]{"CAT", "Cat", "cat", "CaT"}) {
            mockMvc.perform(get("/api/stickersets/search")
                    .param("query", query)
                    .param("page", "0")
                    .param("size", "20")
                    .header("X-Telegram-Init-Data", validInitData))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
        }
    }
    
    @Test
    @Story("Пагинация")
    @DisplayName("Поиск поддерживает пагинацию")
    @Description("Проверка работы параметров page и size")
    void testSearchPagination() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "cat")
                .param("page", "0")
                .param("size", "2")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)));
    }
    
    @Test
    @Story("Пустой запрос")
    @DisplayName("Поиск с пустым query возвращает все стикерсеты")
    @Description("Если query пустой, должны вернуться все публичные активные стикерсеты")
    void testSearchWithEmptyQuery() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "")
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(4)))); // Все публичные активные (4)
    }
    
    @Test
    @Story("Несуществующий запрос")
    @DisplayName("Поиск несуществующего текста возвращает пустой результат")
    @Description("Если ничего не найдено, должен вернуться пустой список")
    void testSearchNoResults() throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", "xyznonexistent12345")
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
    
    // Вспомогательные методы
    
    private void createTestUserAndProfile() {
        UserEntity user = new UserEntity();
        user.setId(TEST_USER_ID); // Telegram ID хранится в поле id
        user.setUsername("TestUser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setLanguageCode("en");
        userRepository.save(user);
        
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(TEST_USER_ID);
        profile.setArtBalance(0L); // Long, а не int
        userProfileRepository.save(profile);
    }
    
    private void cleanupTestData() {
        // Удаляем все стикерсеты тестового пользователя
        stickerSetRepository.deleteAll(
            stickerSetRepository.findByUserId(TEST_USER_ID)
        );
        
        // Удаляем тестового пользователя и профиль
        userProfileRepository.deleteById(TEST_USER_ID);
        userRepository.deleteById(TEST_USER_ID);
    }
}


package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.testdata.TestConstants;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.testdata.StickerSetTestBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import com.example.sticker_art_gallery.util.TelegramInitDataGenerator;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StickerSetSearchIntegrationTest {
    
    // Используем константы из TestConstants
    private static final String TEST_STICKERSET_CAT = TestConstants.TEST_STICKERSET_CAT;
    private static final String TEST_STICKERSET_DOG = TestConstants.TEST_STICKERSET_DOG;
    private static final String TEST_STICKERSET_ANIMAL = TestConstants.TEST_STICKERSET_ANIMAL;
    private static final String TEST_STICKERSET_PRIVATE = TestConstants.TEST_STICKERSET_PRIVATE;
    private static final String TEST_STICKERSET_BLOCKED = TestConstants.TEST_STICKERSET_BLOCKED_CAT;
    private static final String TEST_STICKERSET_OFFICIAL = TestConstants.TEST_STICKERSET_OFFICIAL_CAT;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private StickerSetRepository stickerSetRepository;
    
    @Autowired
    private StickerSetTestSteps testSteps;
    
    private String validInitData;
    private static final Long TEST_USER_ID = TestDataBuilder.TEST_USER_ID;
    
    @BeforeAll
    void setUp() throws Exception {
        // Создаем пользователя и профиль один раз для всех тестов
        testSteps.createTestUserAndProfile(TEST_USER_ID);
        
        String botToken = appConfig.getTelegram().getBotToken();
        validInitData = TelegramInitDataGenerator.builder()
                .botToken(botToken)
                .userId(TEST_USER_ID)
                .username(TestConstants.TEST_USERNAME_TEST_USER)
                .firstName(TestConstants.TEST_FIRST_NAME_TEST)
                .lastName(TestConstants.TEST_LAST_NAME_USER)
                .languageCode(TestConstants.TEST_LANGUAGE_CODE_EN)
                .build();
        
        // Удаляем существующие тестовые стикерсеты (на случай предыдущих запусков)
        testSteps.cleanupTestStickerSets(
            TEST_STICKERSET_CAT,
            TEST_STICKERSET_DOG,
            TEST_STICKERSET_ANIMAL,
            TEST_STICKERSET_PRIVATE,
            TEST_STICKERSET_BLOCKED,
            TEST_STICKERSET_OFFICIAL
        );
        
        // Создаем тестовые стикерсеты для поиска один раз для всех тестов
        createTestStickerSets();
    }
    
    @AfterAll
    void tearDown() {
        // Безопасная очистка: удаляем только тестовые стикерсеты по именам
        // НЕ удаляем пользователя и профиль, так как они могут использоваться в продакшене
        testSteps.cleanupTestStickerSets(
            TEST_STICKERSET_CAT,
            TEST_STICKERSET_DOG,
            TEST_STICKERSET_ANIMAL,
            TEST_STICKERSET_PRIVATE,
            TEST_STICKERSET_BLOCKED,
            TEST_STICKERSET_OFFICIAL
        );
    }
    
    private void createTestStickerSets() {
        // Стикерсет с "cat" в title
        StickerSet catStickers = StickerSetTestBuilder.builder()
                .withUserId(TEST_USER_ID)
                .withTitle("Funny Cat Stickers")
                .withDescription("Collection of cute cats")
                .withName(TEST_STICKERSET_CAT)
                .build();
        stickerSetRepository.save(catStickers);
        
        // Стикерсет с "dog" в title
        StickerSet dogStickers = StickerSetTestBuilder.builder()
                .withUserId(TEST_USER_ID)
                .withTitle("Happy Dogs Pack")
                .withDescription("Best dog stickers ever")
                .withName(TEST_STICKERSET_DOG)
                .build();
        stickerSetRepository.save(dogStickers);
        
        // Стикерсет с "cat" в description
        StickerSet animalStickers = StickerSetTestBuilder.builder()
                .withUserId(TEST_USER_ID)
                .withTitle("Animal Kingdom")
                .withDescription("Features cute cat and dog images")
                .withName(TEST_STICKERSET_ANIMAL)
                .build();
        stickerSetRepository.save(animalStickers);
        
        // Приватный стикерсет (не должен находиться)
        StickerSet privateStickers = StickerSetTestBuilder.builder()
                .withUserId(TEST_USER_ID)
                .withTitle("Private Cat Collection")
                .withDescription("My personal cat stickers")
                .withName(TEST_STICKERSET_PRIVATE)
                .asPrivate()
                .build();
        stickerSetRepository.save(privateStickers);
        
        // Заблокированный стикерсет (не должен находиться)
        StickerSet blockedStickers = StickerSetTestBuilder.builder()
                .withUserId(TEST_USER_ID)
                .withTitle("Blocked Cat Stickers")
                .withDescription("These cats are blocked")
                .withName(TEST_STICKERSET_BLOCKED)
                .asBlocked("Test block reason")
                .build();
        stickerSetRepository.save(blockedStickers);
        
        // Официальный стикерсет
        StickerSet officialStickers = StickerSetTestBuilder.builder()
                .withUserId(TEST_USER_ID)
                .withTitle("Official Cat Pack")
                .withDescription("Official Telegram cats")
                .withName(TEST_STICKERSET_OFFICIAL)
                .asOfficial()
                .build();
        stickerSetRepository.save(officialStickers);
    }
    
    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
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
    
    @ParameterizedTest
    @ValueSource(strings = {"CAT", "Cat", "cat", "CaT"})
    @Story("Регистронезависимый поиск")
    @DisplayName("Поиск не зависит от регистра: {0} -> результаты найдены")
    @Description("Должен найти результаты независимо от регистра запроса")
    @Tag("search")
    void testSearchCaseInsensitive(String query) throws Exception {
        mockMvc.perform(get("/api/stickersets/search")
                .param("query", query)
                .param("page", "0")
                .param("size", "20")
                .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
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
}


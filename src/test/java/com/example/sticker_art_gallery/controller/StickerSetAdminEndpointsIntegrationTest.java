package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.testdata.TestConstants;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.testdata.StickerSetTestBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Стикерсеты")
@Feature("Админ-эндпоинты: официальный статус и автор")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StickerSetAdminEndpointsIntegrationTest {

    private static final String TEST_STICKERSET_NAME = TestConstants.TEST_STICKERSET_ADMIN;

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    private String userInitData;
    private String adminInitData;
    private final Long userId = TestDataBuilder.TEST_USER_ID;
    private final Long adminUserId = 999999001L;

    private Long testStickerSetId;

    @BeforeAll
    void setUp() {
        // Создаем пользователей один раз для всех тестов
        testSteps.createTestUserAndProfile(userId);
        testSteps.createTestUserAndProfile(adminUserId);
        testSteps.makeAdmin(adminUserId);
        userInitData = testSteps.createValidInitData(userId);
        adminInitData = testSteps.createValidInitData(adminUserId);

        // Удаляем существующий тестовый стикерсет (на случай предыдущих запусков)
        testSteps.cleanupTestStickerSets(TEST_STICKERSET_NAME);

        // Создаем стикерсет напрямую в репозитории, чтобы не зависеть от внешнего Bot API
        StickerSet ss = StickerSetTestBuilder.builder()
                .withUserId(userId)
                .withTitle("Test Set")
                .withName(TEST_STICKERSET_NAME)
                .build();
        testStickerSetId = stickerSetRepository.save(ss).getId();
    }

    @AfterAll
    void tearDown() {
        // Безопасная очистка: удаляем только тестовый стикерсет по имени
        // НЕ используем deleteAll() для безопасности продакшн БД
        testSteps.cleanupTestStickerSets(TEST_STICKERSET_NAME);
    }

    @Test
    @Story("Официальный статус")
    @DisplayName("Пользователь без прав не может выставить официальный статус (403)")
    void userCannotMarkOfficial() throws Exception {
        ResultActions result = testSteps.markOfficial(testStickerSetId, userInitData);
        result.andExpect(status().isForbidden());
    }

    @Test
    @Story("Официальный статус")
    @DisplayName("Админ выставляет официальный статус и снимает его")
    void adminCanToggleOfficial() throws Exception {
        // set official
        testSteps.markOfficial(testStickerSetId, adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("OFFICIAL"))
                .andExpect(jsonPath("$.isOfficial").value(true)); // обратная совместимость

        // unset official
        testSteps.markUnofficial(testStickerSetId, adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("USER"))
                .andExpect(jsonPath("$.isOfficial").value(false)); // обратная совместимость
    }

    @Test
    @Story("Автор")
    @DisplayName("Админ устанавливает authorId и очищает его")
    void adminCanSetAndClearAuthor() throws Exception {
        // set author
        testSteps.setAuthor(testStickerSetId, TestConstants.TEST_AUTHOR_ID_123456789, adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(TestConstants.TEST_AUTHOR_ID_123456789));

        // clear author
        testSteps.clearAuthor(testStickerSetId, adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").doesNotExist());
    }

    @Test
    @Story("Автор")
    @DisplayName("Пользователь без прав не может установить автора (403)")
    void userCannotSetAuthor() throws Exception {
        testSteps.setAuthor(testStickerSetId, 123L, userInitData)
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, -5L, -100L, 0L})
    @Story("Автор")
    @DisplayName("Валидация authorId: некорректные значения ({0}) -> 400")
    @Tag("validation")
    void setAuthorValidation_InvalidAuthorIds_ShouldReturn400(long invalidAuthorId) throws Exception {
        testSteps.setAuthor(testStickerSetId, invalidAuthorId, adminInitData)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}



package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
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
class StickerSetAdminEndpointsIntegrationTest {

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    private String userInitData;
    private String adminInitData;
    private final Long userId = TestDataBuilder.TEST_USER_ID;
    private final Long adminUserId = 999999001L;

    private Long testStickerSetId;

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(userId);
        testSteps.createTestUserAndProfile(adminUserId);
        testSteps.makeAdmin(adminUserId);
        userInitData = testSteps.createValidInitData(userId);
        adminInitData = testSteps.createValidInitData(adminUserId);

        // Создаем стикерсет напрямую в репозитории, чтобы не зависеть от внешнего Bot API
        StickerSet ss = new StickerSet();
        ss.setUserId(userId);
        ss.setTitle("Test Set");
        ss.setName("test_set_by_StickerGalleryBot");
        ss.setIsPublic(true);
        ss.setIsBlocked(false);
        ss.setIsOfficial(false);
        ss.setAuthorId(null);
        testStickerSetId = stickerSetRepository.save(ss).getId();
    }

    @AfterEach
    void tearDown() {
        stickerSetRepository.deleteAll();
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
                .andExpect(jsonPath("$.isOfficial").value(true));

        // unset official
        testSteps.markUnofficial(testStickerSetId, adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(false));
    }

    @Test
    @Story("Автор")
    @DisplayName("Админ устанавливает authorId и очищает его")
    void adminCanSetAndClearAuthor() throws Exception {
        // set author
        testSteps.setAuthor(testStickerSetId, 123456789L, adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(123456789L));

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

    @Test
    @Story("Автор")
    @DisplayName("Валидация authorId: отрицательное значение -> 400")
    void setAuthorValidation() throws Exception {
        testSteps.setAuthor(testStickerSetId, -5L, adminInitData)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}



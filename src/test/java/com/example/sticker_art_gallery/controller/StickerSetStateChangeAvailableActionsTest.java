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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Стикерсеты")
@Feature("Эндпоинты изменения состояния: availableActions в ответах")
@DisplayName("Тесты на наличие availableActions в ответах эндпоинтов изменения состояния")
class StickerSetStateChangeAvailableActionsTest {

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
        ss.setIsPublic(false); // Начинаем с приватного
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
    @Story("Publish эндпоинт")
    @DisplayName("POST /api/stickersets/{id}/publish возвращает availableActions")
    @Severity(SeverityLevel.CRITICAL)
    void publishStickerSet_ShouldReturnAvailableActions() throws Exception {
        // Given: приватный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        Assertions.assertFalse(stickerSet.getIsPublic(), "Стикерсет должен быть приватным");

        // When: публикуем стикерсет
        ResultActions result = testSteps.publishStickerSet(testStickerSetId, userInitData);

        // Then: проверяем, что ответ содержит availableActions
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStickerSetId))
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    @Story("Unpublish эндпоинт")
    @DisplayName("POST /api/stickersets/{id}/unpublish возвращает availableActions")
    @Severity(SeverityLevel.CRITICAL)
    void unpublishStickerSet_ShouldReturnAvailableActions() throws Exception {
        // Given: публичный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        stickerSet.setIsPublic(true);
        stickerSetRepository.save(stickerSet);

        // When: скрываем стикерсет
        ResultActions result = testSteps.unpublishStickerSet(testStickerSetId, userInitData);

        // Then: проверяем, что ответ содержит availableActions
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStickerSetId))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    @Story("Block эндпоинт")
    @DisplayName("PUT /api/stickersets/{id}/block возвращает availableActions")
    @Severity(SeverityLevel.CRITICAL)
    void blockStickerSet_ShouldReturnAvailableActions() throws Exception {
        // Given: незаблокированный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        Assertions.assertFalse(stickerSet.getIsBlocked(), "Стикерсет должен быть незаблокирован");

        // When: блокируем стикерсет (только админ может)
        ResultActions result = testSteps.blockStickerSet(testStickerSetId, adminInitData, "Test reason");

        // Then: проверяем, что ответ содержит availableActions
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStickerSetId))
                .andExpect(jsonPath("$.isBlocked").value(true))
                .andExpect(jsonPath("$.blockReason").value("Test reason"))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    @Story("Block эндпоинт")
    @DisplayName("PUT /api/stickersets/{id}/block без причины возвращает availableActions")
    @Severity(SeverityLevel.CRITICAL)
    void blockStickerSet_WithoutReason_ShouldReturnAvailableActions() throws Exception {
        // Given: незаблокированный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        Assertions.assertFalse(stickerSet.getIsBlocked(), "Стикерсет должен быть незаблокирован");

        // When: блокируем стикерсет без причины
        ResultActions result = testSteps.blockStickerSet(testStickerSetId, adminInitData);

        // Then: проверяем, что ответ содержит availableActions
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStickerSetId))
                .andExpect(jsonPath("$.isBlocked").value(true))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    @Story("Unblock эндпоинт")
    @DisplayName("PUT /api/stickersets/{id}/unblock возвращает availableActions")
    @Severity(SeverityLevel.CRITICAL)
    void unblockStickerSet_ShouldReturnAvailableActions() throws Exception {
        // Given: заблокированный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        stickerSet.setIsBlocked(true);
        stickerSet.setBlockReason("Test block reason");
        stickerSetRepository.save(stickerSet);

        // When: разблокируем стикерсет (только админ может)
        ResultActions result = testSteps.unblockStickerSet(testStickerSetId, adminInitData);

        // Then: проверяем, что ответ содержит availableActions
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStickerSetId))
                .andExpect(jsonPath("$.isBlocked").value(false))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    @Story("Publish эндпоинт")
    @DisplayName("POST /api/stickersets/{id}/publish: availableActions содержит правильные действия для владельца")
    @Severity(SeverityLevel.NORMAL)
    void publishStickerSet_AvailableActions_ShouldContainCorrectActionsForOwner() throws Exception {
        // Given: приватный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        Assertions.assertFalse(stickerSet.getIsPublic(), "Стикерсет должен быть приватным");

        // When: публикуем стикерсет
        ResultActions result = testSteps.publishStickerSet(testStickerSetId, userInitData);

        // Then: проверяем, что availableActions содержит ожидаемые действия для владельца
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                // После публикации приватного стикерсета владелец должен иметь возможность скрыть его
                .andExpect(jsonPath("$.availableActions[?(@ == 'UNPUBLISH')]").exists());
    }

    @Test
    @Story("Unpublish эндпоинт")
    @DisplayName("POST /api/stickersets/{id}/unpublish: availableActions содержит правильные действия для владельца")
    @Severity(SeverityLevel.NORMAL)
    void unpublishStickerSet_AvailableActions_ShouldContainCorrectActionsForOwner() throws Exception {
        // Given: публичный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        stickerSet.setIsPublic(true);
        stickerSetRepository.save(stickerSet);

        // When: скрываем стикерсет
        ResultActions result = testSteps.unpublishStickerSet(testStickerSetId, userInitData);

        // Then: проверяем, что availableActions содержит ожидаемые действия для владельца
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                // После скрытия публичного стикерсета владелец должен иметь возможность опубликовать его
                .andExpect(jsonPath("$.availableActions[?(@ == 'PUBLISH')]").exists());
    }

    @Test
    @Story("Block эндпоинт")
    @DisplayName("PUT /api/stickersets/{id}/block: availableActions содержит UNBLOCK для админа")
    @Severity(SeverityLevel.NORMAL)
    void blockStickerSet_AvailableActions_ShouldContainUnblockForAdmin() throws Exception {
        // Given: незаблокированный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        Assertions.assertFalse(stickerSet.getIsBlocked(), "Стикерсет должен быть незаблокирован");

        // When: блокируем стикерсет
        ResultActions result = testSteps.blockStickerSet(testStickerSetId, adminInitData, "Test reason");

        // Then: проверяем, что availableActions содержит UNBLOCK для админа
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'UNBLOCK')]").exists());
    }

    @Test
    @Story("Unblock эндпоинт")
    @DisplayName("PUT /api/stickersets/{id}/unblock: availableActions не содержит UNBLOCK после разблокировки")
    @Severity(SeverityLevel.NORMAL)
    void unblockStickerSet_AvailableActions_ShouldNotContainUnblockAfterUnblocking() throws Exception {
        // Given: заблокированный стикерсет
        StickerSet stickerSet = stickerSetRepository.findById(testStickerSetId).orElseThrow();
        stickerSet.setIsBlocked(true);
        stickerSet.setBlockReason("Test block reason");
        stickerSetRepository.save(stickerSet);

        // When: разблокируем стикерсет
        ResultActions result = testSteps.unblockStickerSet(testStickerSetId, adminInitData);

        // Then: проверяем, что availableActions не содержит UNBLOCK (так как стикерсет уже разблокирован)
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'UNBLOCK')]").doesNotExist());
    }
}

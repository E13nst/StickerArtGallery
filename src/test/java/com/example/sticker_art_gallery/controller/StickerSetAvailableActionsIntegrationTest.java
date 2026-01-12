package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StickerSetAction;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@Epic("Стикерсеты")
@Feature("AvailableActions в ответах эндпоинтов изменения состояния")
class StickerSetAvailableActionsIntegrationTest {

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    private String userInitData;
    private String adminInitData;
    private final Long userId = TestDataBuilder.TEST_USER_ID;
    private final Long adminUserId = 999999001L;
    private final Long otherUserId = 999999002L;

    private Long privateStickerSetId;
    private Long publicStickerSetId;
    private Long blockedStickerSetId;

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(userId);
        testSteps.createTestUserAndProfile(adminUserId);
        testSteps.createTestUserAndProfile(otherUserId);
        testSteps.makeAdmin(adminUserId);
        userInitData = testSteps.createValidInitData(userId);
        adminInitData = testSteps.createValidInitData(adminUserId);

        // Создаем приватный стикерсет
        StickerSet privateStickerSet = new StickerSet();
        privateStickerSet.setUserId(userId);
        privateStickerSet.setTitle("Private Set");
        privateStickerSet.setName("private_set_by_StickerGalleryBot");
        privateStickerSet.setState(StickerSetState.ACTIVE);
        privateStickerSet.setVisibility(StickerSetVisibility.PRIVATE);
        privateStickerSet.setType(StickerSetType.USER);
        privateStickerSet.setAuthorId(userId);
        privateStickerSetId = stickerSetRepository.save(privateStickerSet).getId();

        // Создаем публичный стикерсет
        StickerSet publicStickerSet = new StickerSet();
        publicStickerSet.setUserId(userId);
        publicStickerSet.setTitle("Public Set");
        publicStickerSet.setName("public_set_by_StickerGalleryBot");
        publicStickerSet.setState(StickerSetState.ACTIVE);
        publicStickerSet.setVisibility(StickerSetVisibility.PUBLIC);
        publicStickerSet.setType(StickerSetType.USER);
        publicStickerSet.setAuthorId(userId);
        publicStickerSetId = stickerSetRepository.save(publicStickerSet).getId();

        // Создаем заблокированный стикерсет
        StickerSet blockedStickerSet = new StickerSet();
        blockedStickerSet.setUserId(userId);
        blockedStickerSet.setTitle("Blocked Set");
        blockedStickerSet.setName("blocked_set_by_StickerGalleryBot");
        blockedStickerSet.setState(StickerSetState.BLOCKED);
        blockedStickerSet.setVisibility(StickerSetVisibility.PUBLIC);
        blockedStickerSet.setType(StickerSetType.USER);
        blockedStickerSet.setBlockReason("Test block");
        blockedStickerSet.setAuthorId(userId);
        blockedStickerSetId = stickerSetRepository.save(blockedStickerSet).getId();
    }

    @AfterEach
    void tearDown() {
        // Удаляем только тестовые стикерсеты, созданные в setUp()
        if (privateStickerSetId != null) {
            stickerSetRepository.findById(privateStickerSetId).ifPresent(stickerSetRepository::delete);
        }
        if (publicStickerSetId != null) {
            stickerSetRepository.findById(publicStickerSetId).ifPresent(stickerSetRepository::delete);
        }
        if (blockedStickerSetId != null) {
            stickerSetRepository.findById(blockedStickerSetId).ifPresent(stickerSetRepository::delete);
        }
    }

    @Test
    @Story("Publish эндпоинт")
    @DisplayName("Publish возвращает availableActions с UNPUBLISH для автора")
    @Severity(SeverityLevel.CRITICAL)
    void publishReturnsAvailableActionsWithUnpublish() throws Exception {
        ResultActions result = testSteps.publishStickerSet(privateStickerSetId, userInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'UNPUBLISH')]").exists())
                .andExpect(jsonPath("$.availableActions[?(@ == 'DELETE')]").exists());
    }

    @Test
    @Story("Unpublish эндпоинт")
    @DisplayName("Unpublish возвращает availableActions с PUBLISH для автора")
    @Severity(SeverityLevel.CRITICAL)
    void unpublishReturnsAvailableActionsWithPublish() throws Exception {
        ResultActions result = testSteps.unpublishStickerSet(publicStickerSetId, userInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'PUBLISH')]").exists())
                .andExpect(jsonPath("$.availableActions[?(@ == 'DELETE')]").exists());
    }

    @Test
    @Story("Block эндпоинт")
    @DisplayName("Block возвращает availableActions с UNBLOCK для админа")
    @Severity(SeverityLevel.CRITICAL)
    void blockReturnsAvailableActionsWithUnblock() throws Exception {
        ResultActions result = testSteps.blockStickerSet(publicStickerSetId, adminInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'UNBLOCK')]").exists());
    }

    @Test
    @Story("Unblock эндпоинт")
    @DisplayName("Unblock возвращает availableActions с BLOCK для админа")
    @Severity(SeverityLevel.CRITICAL)
    void unblockReturnsAvailableActionsWithBlock() throws Exception {
        ResultActions result = testSteps.unblockStickerSet(blockedStickerSetId, adminInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'BLOCK')]").exists());
    }

    @Test
    @Story("Publish эндпоинт")
    @DisplayName("Publish от админа возвращает availableActions с BLOCK")
    @Severity(SeverityLevel.NORMAL)
    void publishByAdminReturnsAvailableActionsWithBlock() throws Exception {
        ResultActions result = testSteps.publishStickerSet(privateStickerSetId, adminInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'BLOCK')]").exists());
    }

    @Test
    @Story("Unpublish эндпоинт")
    @DisplayName("Unpublish от админа возвращает availableActions с BLOCK")
    @Severity(SeverityLevel.NORMAL)
    void unpublishByAdminReturnsAvailableActionsWithBlock() throws Exception {
        ResultActions result = testSteps.unpublishStickerSet(publicStickerSetId, adminInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'BLOCK')]").exists());
    }

    @Test
    @Story("Block эндпоинт")
    @DisplayName("Block с причиной возвращает availableActions")
    @Severity(SeverityLevel.NORMAL)
    void blockWithReasonReturnsAvailableActions() throws Exception {
        ResultActions result = testSteps.blockStickerSet(publicStickerSetId, "Test reason", adminInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true))
                .andExpect(jsonPath("$.blockReason").value("Test reason"))
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[?(@ == 'UNBLOCK')]").exists());
    }

    @Test
    @Story("AvailableActions")
    @DisplayName("AvailableActions содержит только допустимые значения")
    @Severity(SeverityLevel.NORMAL)
    void availableActionsContainsOnlyValidValues() throws Exception {
        ResultActions result = testSteps.publishStickerSet(privateStickerSetId, userInitData);
        
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions").exists())
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions[*]").value(
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.in(java.util.Arrays.asList(
                                        StickerSetAction.DELETE.name(),
                                        StickerSetAction.BLOCK.name(),
                                        StickerSetAction.UNBLOCK.name(),
                                        StickerSetAction.PUBLISH.name(),
                                        StickerSetAction.UNPUBLISH.name()
                                ))
                        )
                ));
    }
}


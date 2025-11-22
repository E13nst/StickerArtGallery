package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.testdata.TestUsers;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@Epic("Стикерсеты")
@Feature("Доступ к авторским стикерсетам")
class StickerSetAuthorVisibilityIntegrationTest {

    private static final String AUTHOR_PUBLIC_PACK = "author_public_pack_by_testbot";
    private static final String AUTHOR_PRIVATE_PACK = "author_private_pack_by_testbot";
    private static final String AUTHOR_BLOCKED_PACK = "author_blocked_pack_by_testbot";

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    private String adminInitData;
    private String authorInitData;
    private String viewerInitData;

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(TestUsers.ADMIN.id());
        testSteps.createTestUserAndProfile(TestUsers.OWNER.id());
        testSteps.createTestUserAndProfile(TestUsers.VIEWER.id());
        testSteps.makeAdmin(TestUsers.ADMIN.id());

        adminInitData = testSteps.createValidInitData(TestUsers.ADMIN.id());
        authorInitData = testSteps.createValidInitData(TestUsers.OWNER.id());
        viewerInitData = testSteps.createValidInitData(TestUsers.VIEWER.id());

        // Удаляем существующие тестовые стикерсеты
        stickerSetRepository.findByNameIgnoreCase(AUTHOR_PUBLIC_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(AUTHOR_PRIVATE_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(AUTHOR_BLOCKED_PACK).ifPresent(stickerSetRepository::delete);

        stickerSetRepository.saveAll(List.of(
                buildStickerSet(TestUsers.VIEWER.id(), TestUsers.OWNER.id(), AUTHOR_PUBLIC_PACK, true),
                buildStickerSet(TestUsers.VIEWER.id(), TestUsers.OWNER.id(), AUTHOR_PRIVATE_PACK, false),
                buildBlockedStickerSet(TestUsers.VIEWER.id(), TestUsers.OWNER.id(), AUTHOR_BLOCKED_PACK)
        ));
    }

    @AfterEach
    void tearDown() {
        // Удаляем тестовые стикерсеты по именам
        stickerSetRepository.findByNameIgnoreCase(AUTHOR_PUBLIC_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(AUTHOR_PRIVATE_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(AUTHOR_BLOCKED_PACK).ifPresent(stickerSetRepository::delete);
    }

    @Test
    @Story("Администратор просматривает авторские стикерсеты")
    @DisplayName("Администратор видит приватные авторские стикерсеты")
    void adminShouldSeeAuthorStickerSetsIncludingPrivate() throws Exception {
        testSteps.getStickerSetsByAuthor(TestUsers.OWNER.id(), adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.is(2)))
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItems(
                        AUTHOR_PUBLIC_PACK,
                        AUTHOR_PRIVATE_PACK
                )));
    }

    @Test
    @Story("Автор просматривает свои стикерсеты")
    @DisplayName("Автор видит все свои стикерсеты")
    void authorShouldSeeOwnStickerSetsIncludingPrivate() throws Exception {
        testSteps.getStickerSetsByAuthor(TestUsers.OWNER.id(), authorInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.is(2)))
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItems(
                        AUTHOR_PUBLIC_PACK,
                        AUTHOR_PRIVATE_PACK
                )));
    }

    @Test
    @Story("Обычный пользователь просматривает авторские стикерсеты")
    @DisplayName("Неавтор и неадмин видит только публичные авторские стикерсеты")
    void viewerShouldSeeOnlyPublicAuthorStickerSets() throws Exception {
        testSteps.getStickerSetsByAuthor(TestUsers.OWNER.id(), viewerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].name").value(AUTHOR_PUBLIC_PACK));
    }

    @Test
    @Story("Заблокированные стикерсеты не отображаются")
    @DisplayName("Заблокированные стикерсеты не отображаются в /api/stickersets/author/{authorId}")
    @Description("Заблокированные стикерсеты не должны отображаться в списке авторских стикерсетов, " +
                 "даже для автора или администратора")
    @Severity(SeverityLevel.CRITICAL)
    void blockedStickerSetsShouldNotBeVisibleInAuthorEndpoint() throws Exception {
        // Проверяем для автора
        testSteps.getStickerSetsByAuthor(TestUsers.OWNER.id(), authorInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", Matchers.not(Matchers.hasItem(AUTHOR_BLOCKED_PACK))))
                .andExpect(jsonPath("$.content[*].state", Matchers.not(Matchers.hasItem("BLOCKED"))));

        // Проверяем для администратора
        testSteps.getStickerSetsByAuthor(TestUsers.OWNER.id(), adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", Matchers.not(Matchers.hasItem(AUTHOR_BLOCKED_PACK))))
                .andExpect(jsonPath("$.content[*].state", Matchers.not(Matchers.hasItem("BLOCKED"))));

        // Проверяем для обычного пользователя
        testSteps.getStickerSetsByAuthor(TestUsers.OWNER.id(), viewerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", Matchers.not(Matchers.hasItem(AUTHOR_BLOCKED_PACK))))
                .andExpect(jsonPath("$.content[*].state", Matchers.not(Matchers.hasItem("BLOCKED"))));
    }

    private StickerSet buildStickerSet(Long userId, Long authorId, String name, boolean isPublic) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setAuthorId(authorId);
        stickerSet.setTitle(name + "_title");
        stickerSet.setName(name);
        stickerSet.setState(StickerSetState.ACTIVE);
        stickerSet.setVisibility(isPublic ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE);
        stickerSet.setType(StickerSetType.USER);
        stickerSet.setLikesCount(0);
        return stickerSet;
    }

    private StickerSet buildBlockedStickerSet(Long userId, Long authorId, String name) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setAuthorId(authorId);
        stickerSet.setTitle(name + "_title");
        stickerSet.setName(name);
        stickerSet.setState(StickerSetState.BLOCKED);
        stickerSet.setVisibility(StickerSetVisibility.PUBLIC);
        stickerSet.setType(StickerSetType.USER);
        stickerSet.setLikesCount(0);
        stickerSet.setBlockReason("Test block reason");
        return stickerSet;
    }
}


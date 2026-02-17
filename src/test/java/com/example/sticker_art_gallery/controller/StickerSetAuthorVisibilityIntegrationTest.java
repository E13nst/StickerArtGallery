package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.testdata.TestConstants;
import com.example.sticker_art_gallery.testdata.TestUsers;
import com.example.sticker_art_gallery.testdata.StickerSetTestBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StickerSetAuthorVisibilityIntegrationTest {

    // Используем константы из TestConstants
    private static final String AUTHOR_PUBLIC_PACK = TestConstants.TEST_STICKERSET_AUTHOR_PUBLIC;
    private static final String AUTHOR_PRIVATE_PACK = TestConstants.TEST_STICKERSET_AUTHOR_PRIVATE;
    private static final String AUTHOR_BLOCKED_PACK = TestConstants.TEST_STICKERSET_AUTHOR_BLOCKED;

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    private String adminInitData;
    private String authorInitData;
    private String viewerInitData;

    @BeforeAll
    void setUp() {
        // Создаем пользователей один раз для всех тестов
        testSteps.createTestUserAndProfile(TestUsers.ADMIN.id());
        testSteps.createTestUserAndProfile(TestUsers.OWNER.id());
        testSteps.createTestUserAndProfile(TestUsers.VIEWER.id());
        testSteps.makeAdmin(TestUsers.ADMIN.id());

        adminInitData = testSteps.createValidInitData(TestUsers.ADMIN.id());
        authorInitData = testSteps.createValidInitData(TestUsers.OWNER.id());
        viewerInitData = testSteps.createValidInitData(TestUsers.VIEWER.id());

        // Удаляем существующие тестовые стикерсеты (на случай предыдущих запусков)
        testSteps.cleanupTestStickerSets(
            AUTHOR_PUBLIC_PACK,
            AUTHOR_PRIVATE_PACK,
            AUTHOR_BLOCKED_PACK
        );

        // Создаем тестовые стикерсеты один раз для всех тестов используя StickerSetTestBuilder
        stickerSetRepository.saveAll(List.of(
                StickerSetTestBuilder.builder()
                        .withUserId(TestUsers.OWNER.id())
                        .withIsVerified(true)
                        .withTitle(AUTHOR_PUBLIC_PACK + "_title")
                        .withName(AUTHOR_PUBLIC_PACK)
                        .build(),
                StickerSetTestBuilder.builder()
                        .withUserId(TestUsers.OWNER.id())
                        .withIsVerified(true)
                        .withTitle(AUTHOR_PRIVATE_PACK + "_title")
                        .withName(AUTHOR_PRIVATE_PACK)
                        .asPrivate()
                        .build(),
                StickerSetTestBuilder.builder()
                        .withUserId(TestUsers.OWNER.id())
                        .withIsVerified(true)
                        .withTitle(AUTHOR_BLOCKED_PACK + "_title")
                        .withName(AUTHOR_BLOCKED_PACK)
                        .asBlocked("Test block reason")
                        .build()
        ));
    }

    @AfterAll
    void tearDown() {
        // Удаляем тестовые стикерсеты один раз после всех тестов
        testSteps.cleanupTestStickerSets(
            AUTHOR_PUBLIC_PACK,
            AUTHOR_PRIVATE_PACK,
            AUTHOR_BLOCKED_PACK
        );
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

}


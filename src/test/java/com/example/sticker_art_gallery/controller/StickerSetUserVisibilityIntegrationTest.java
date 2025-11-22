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
@Feature("Доступ к стикерсетам пользователя")
class StickerSetUserVisibilityIntegrationTest {

    private static final String OWNER_PUBLIC_PACK = "owner_public_pack_by_testbot";
    private static final String OWNER_PRIVATE_PACK = "owner_private_pack_by_testbot";
    private static final String OWNER_BLOCKED_PACK = "owner_blocked_pack_by_testbot";

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    private String adminInitData;
    private String ownerInitData;
    private String viewerInitData;

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(TestUsers.ADMIN.id());
        testSteps.createTestUserAndProfile(TestUsers.OWNER.id());
        testSteps.createTestUserAndProfile(TestUsers.VIEWER.id());
        testSteps.makeAdmin(TestUsers.ADMIN.id());

        adminInitData = testSteps.createValidInitData(TestUsers.ADMIN.id());
        ownerInitData = testSteps.createValidInitData(TestUsers.OWNER.id());
        viewerInitData = testSteps.createValidInitData(TestUsers.VIEWER.id());

        // Удаляем существующие тестовые стикерсеты
        stickerSetRepository.findByNameIgnoreCase(OWNER_PUBLIC_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(OWNER_PRIVATE_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(OWNER_BLOCKED_PACK).ifPresent(stickerSetRepository::delete);

        stickerSetRepository.saveAll(List.of(
                buildStickerSet(TestUsers.OWNER.id(), OWNER_PUBLIC_PACK, true),
                buildStickerSet(TestUsers.OWNER.id(), OWNER_PRIVATE_PACK, false),
                buildBlockedStickerSet(TestUsers.OWNER.id(), OWNER_BLOCKED_PACK)
        ));
    }

    @AfterEach
    void tearDown() {
        // Удаляем тестовые стикерсеты по именам
        stickerSetRepository.findByNameIgnoreCase(OWNER_PUBLIC_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(OWNER_PRIVATE_PACK).ifPresent(stickerSetRepository::delete);
        stickerSetRepository.findByNameIgnoreCase(OWNER_BLOCKED_PACK).ifPresent(stickerSetRepository::delete);
    }

    @Test
    @Story("Администратор просматривает чужие стикерсеты")
    @DisplayName("Администратор видит приватные стикерсеты другого пользователя")
    void adminShouldSeePublicAndPrivateStickerSetsOfOtherUser() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), adminInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.is(2)))
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItems(
                        OWNER_PUBLIC_PACK,
                        OWNER_PRIVATE_PACK
                )));
    }

    @Test
    @Story("Обычный пользователь просматривает чужие стикерсеты")
    @DisplayName("Неадминистратор видит только публичные стикерсеты другого пользователя")
    void viewerShouldSeeOnlyPublicStickerSetsOfOtherUser() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), viewerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].name").value(OWNER_PUBLIC_PACK));
    }

    @Test
    @Story("Обычный пользователь просматривает чужие стикерсеты")
    @DisplayName("Другие пользователи не видят заблокированные стикерсеты другого пользователя")
    @Description("Заблокированные стикерсеты не должны отображаться в профиле пользователя для других пользователей")
    @Severity(SeverityLevel.CRITICAL)
    void viewerShouldNotSeeBlockedStickerSetsOfOtherUser() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), viewerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", Matchers.not(Matchers.hasItem(OWNER_BLOCKED_PACK))))
                .andExpect(jsonPath("$.content[*].state", Matchers.not(Matchers.hasItem("BLOCKED"))));
    }

    @Test
    @Story("Владелец просматривает свои стикерсеты")
    @DisplayName("Владелец видит все свои стикерсеты, включая приватные")
    void ownerShouldSeeAllOwnStickerSets() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), ownerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItems(
                        OWNER_PUBLIC_PACK,
                        OWNER_PRIVATE_PACK
                )));
    }

    @Test
    @Story("Владелец просматривает свои стикерсеты")
    @DisplayName("Владелец видит свои заблокированные стикерсеты в профиле")
    @Description("Заблокированные стикерсеты должны отображаться в /api/stickersets/user/{userId} для владельца, " +
                 "чтобы пользователь мог видеть свои заблокированные наборы")
    @Severity(SeverityLevel.CRITICAL)
    void ownerShouldSeeBlockedStickerSetsInOwnProfile() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), ownerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItem(OWNER_BLOCKED_PACK)))
                .andExpect(jsonPath("$.content[?(@.name == '" + OWNER_BLOCKED_PACK + "')].state").value(Matchers.hasItem("BLOCKED")));
    }

    @Test
    @Story("Владелец просматривает свои стикерсеты")
    @DisplayName("Владелец видит все свои стикерсеты, включая заблокированные")
    @Description("Владелец должен видеть все свои стикерсеты: публичные, приватные и заблокированные")
    @Severity(SeverityLevel.CRITICAL)
    void ownerShouldSeeAllOwnStickerSetsIncludingBlocked() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), ownerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItems(
                        OWNER_PUBLIC_PACK,
                        OWNER_PRIVATE_PACK,
                        OWNER_BLOCKED_PACK
                )));
    }

    private StickerSet buildStickerSet(Long userId, String name, boolean isPublic) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(name + "_title");
        stickerSet.setName(name);
        stickerSet.setState(StickerSetState.ACTIVE);
        stickerSet.setVisibility(isPublic ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE);
        stickerSet.setType(StickerSetType.USER);
        stickerSet.setLikesCount(0);
        return stickerSet;
    }

    private StickerSet buildBlockedStickerSet(Long userId, String name) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
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


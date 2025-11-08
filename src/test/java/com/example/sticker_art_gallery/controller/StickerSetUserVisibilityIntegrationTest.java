package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.testdata.TestUsers;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Стикерсеты")
@Feature("Доступ к стикерсетам пользователя")
class StickerSetUserVisibilityIntegrationTest {

    private static final String OWNER_PUBLIC_PACK = "owner_public_pack_by_testbot";
    private static final String OWNER_PRIVATE_PACK = "owner_private_pack_by_testbot";

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

        stickerSetRepository.deleteAll();
        stickerSetRepository.saveAll(List.of(
                buildStickerSet(TestUsers.OWNER.id(), OWNER_PUBLIC_PACK, true),
                buildStickerSet(TestUsers.OWNER.id(), OWNER_PRIVATE_PACK, false)
        ));
    }

    @AfterEach
    void tearDown() {
        stickerSetRepository.deleteAll();
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
    @Story("Владелец просматривает свои стикерсеты")
    @DisplayName("Владелец видит все свои стикерсеты, включая приватные")
    void ownerShouldSeeAllOwnStickerSets() throws Exception {
        testSteps.getStickerSetsByUser(TestUsers.OWNER.id(), ownerInitData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", Matchers.is(2)))
                .andExpect(jsonPath("$.content[*].name", Matchers.hasItems(
                        OWNER_PUBLIC_PACK,
                        OWNER_PRIVATE_PACK
                )));
    }

    private StickerSet buildStickerSet(Long userId, String name, boolean isPublic) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(name + "_title");
        stickerSet.setName(name);
        stickerSet.setIsPublic(isPublic);
        stickerSet.setIsBlocked(false);
        stickerSet.setIsOfficial(false);
        stickerSet.setLikesCount(0);
        return stickerSet;
    }
}


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
@Feature("Доступ к авторским стикерсетам")
class StickerSetAuthorVisibilityIntegrationTest {

    private static final String AUTHOR_PUBLIC_PACK = "author_public_pack_by_testbot";
    private static final String AUTHOR_PRIVATE_PACK = "author_private_pack_by_testbot";

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

        stickerSetRepository.deleteAll();
        stickerSetRepository.saveAll(List.of(
                buildStickerSet(TestUsers.VIEWER.id(), TestUsers.OWNER.id(), AUTHOR_PUBLIC_PACK, true),
                buildStickerSet(TestUsers.VIEWER.id(), TestUsers.OWNER.id(), AUTHOR_PRIVATE_PACK, false)
        ));
    }

    @AfterEach
    void tearDown() {
        stickerSetRepository.deleteAll();
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
}


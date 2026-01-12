package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.repository.LikeRepository;
import com.example.sticker_art_gallery.testdata.TestConstants;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.testdata.StickerSetTestBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Стикерсеты")
@Feature("Фильтры топа по лайкам: officialOnly, authorId, hasAuthorOnly")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StickerSetTopFiltersIntegrationTest {

    // Используем константы из TestConstants
    private static final String TEST_STICKERSET_OFFICIAL = TestConstants.TEST_STICKERSET_TOP_OFFICIAL;
    private static final String TEST_STICKERSET_AUTHORED = TestConstants.TEST_STICKERSET_TOP_AUTHORED;
    private static final String TEST_STICKERSET_PLAIN = TestConstants.TEST_STICKERSET_TOP_PLAIN;

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    @Autowired
    private LikeRepository likeRepository;

    private String initData;
    private final Long userId = TestDataBuilder.TEST_USER_ID;

    private Long sOfficial;
    private Long sAuthored;
    private Long sPlain;

    @BeforeAll
    void setUp() {
        // Создаем пользователя один раз для всех тестов
        testSteps.createTestUserAndProfile(userId);
        initData = testSteps.createValidInitData(userId);

        // Удаляем существующие тестовые стикерсеты (на случай предыдущих запусков)
        testSteps.cleanupTestStickerSets(
            TEST_STICKERSET_OFFICIAL,
            TEST_STICKERSET_AUTHORED,
            TEST_STICKERSET_PLAIN
        );

        // Удаляем лайки для тестовых стикерсетов (если есть)
        cleanupTestLikes();

        // Создаем тестовые стикерсеты один раз для всех тестов используя StickerSetTestBuilder
        // official sticker set with more likes
        StickerSet officialStickerSet = StickerSetTestBuilder.builder()
                .withUserId(userId)
                .withTitle(TEST_STICKERSET_OFFICIAL)
                .withName(TEST_STICKERSET_OFFICIAL)
                .asOfficial()
                .withAuthorId(TestConstants.TEST_AUTHOR_ID_111)
                .build();
        sOfficial = stickerSetRepository.save(officialStickerSet).getId();
        addLikes(sOfficial, 5);

        // authored but not official with fewer likes
        StickerSet authoredStickerSet = StickerSetTestBuilder.builder()
                .withUserId(userId)
                .withTitle(TEST_STICKERSET_AUTHORED)
                .withName(TEST_STICKERSET_AUTHORED)
                .withAuthorId(TestConstants.TEST_AUTHOR_ID_222)
                .build();
        sAuthored = stickerSetRepository.save(authoredStickerSet).getId();
        addLikes(sAuthored, 3);

        // plain (no author, not official) with 1 like
        StickerSet plainStickerSet = StickerSetTestBuilder.builder()
                .withUserId(userId)
                .withTitle(TEST_STICKERSET_PLAIN)
                .withName(TEST_STICKERSET_PLAIN)
                .build();
        sPlain = stickerSetRepository.save(plainStickerSet).getId();
        addLikes(sPlain, 1);
    }

    @AfterAll
    void tearDown() {
        // Безопасная очистка: удаляем лайки для тестовых стикерсетов
        cleanupTestLikes();
        
        // Удаляем только тестовые стикерсеты по именам
        // НЕ используем deleteAll() для безопасности продакшн БД
        testSteps.cleanupTestStickerSets(
            TEST_STICKERSET_OFFICIAL,
            TEST_STICKERSET_AUTHORED,
            TEST_STICKERSET_PLAIN
        );
    }

    private void cleanupTestLikes() {
        // Удаляем лайки только для тестовых стикерсетов
        stickerSetRepository.findByNameIgnoreCase(TEST_STICKERSET_OFFICIAL)
            .ifPresent(ss -> likeRepository.deleteAll(likeRepository.findByStickerSetId(ss.getId())));
        stickerSetRepository.findByNameIgnoreCase(TEST_STICKERSET_AUTHORED)
            .ifPresent(ss -> likeRepository.deleteAll(likeRepository.findByStickerSetId(ss.getId())));
        stickerSetRepository.findByNameIgnoreCase(TEST_STICKERSET_PLAIN)
            .ifPresent(ss -> likeRepository.deleteAll(likeRepository.findByStickerSetId(ss.getId())));
    }

    @Test
    @Story("officialOnly")
    @DisplayName("Топ только официальных")
    void topOfficialOnly() throws Exception {
        testSteps.getTopByLikesWithFilters(true, null, null, initData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isOfficial").value(true));
    }

    @Test
    @Story("authorId")
    @DisplayName("Топ по конкретному authorId")
    void topByAuthorId() throws Exception {
        testSteps.getTopByLikesWithFilters(null, TestConstants.TEST_AUTHOR_ID_222, null, initData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorId").value(TestConstants.TEST_AUTHOR_ID_222));
    }

    @Test
    @Story("hasAuthorOnly")
    @DisplayName("Топ только авторских")
    void topHasAuthorOnly() throws Exception {
        testSteps.getTopByLikesWithFilters(null, null, true, initData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].authorId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));
    }

    private void addLikes(Long stickerSetId, int count) {
        for (int i = 0; i < count; i++) {
            Like like = new Like();
            like.setUserId(TestConstants.TEST_LIKE_USER_ID_BASE + i);
            like.setStickerSet(stickerSetRepository.findById(stickerSetId).orElseThrow());
            likeRepository.save(like);
        }
    }
}



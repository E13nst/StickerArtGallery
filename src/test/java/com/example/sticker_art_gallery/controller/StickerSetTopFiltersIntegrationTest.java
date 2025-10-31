package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.repository.LikeRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
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
class StickerSetTopFiltersIntegrationTest {

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

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(userId);
        initData = testSteps.createValidInitData(userId);
        likeRepository.deleteAll();
        stickerSetRepository.deleteAll();

        // official sticker set with more likes
        sOfficial = saveStickerSet(true, 111L, "top_official_by_StickerGalleryBot");
        addLikes(sOfficial, 5);

        // authored but not official with fewer likes
        sAuthored = saveStickerSet(false, 222L, "top_authored_by_StickerGalleryBot");
        addLikes(sAuthored, 3);

        // plain (no author, not official) with 1 like
        sPlain = saveStickerSet(false, null, "top_plain_by_StickerGalleryBot");
        addLikes(sPlain, 1);
    }

    @AfterEach
    void tearDown() {
        likeRepository.deleteAll();
        stickerSetRepository.deleteAll();
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
        testSteps.getTopByLikesWithFilters(null, 222L, null, initData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorId").value(222L));
    }

    @Test
    @Story("hasAuthorOnly")
    @DisplayName("Топ только авторских")
    void topHasAuthorOnly() throws Exception {
        testSteps.getTopByLikesWithFilters(null, null, true, initData)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].authorId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));
    }

    private Long saveStickerSet(boolean official, Long authorId, String name) {
        StickerSet s = new StickerSet();
        s.setUserId(userId);
        s.setTitle(name);
        s.setName(name);
        s.setIsPublic(true);
        s.setIsBlocked(false);
        s.setIsOfficial(official);
        s.setAuthorId(authorId);
        return stickerSetRepository.save(s).getId();
    }

    private void addLikes(Long stickerSetId, int count) {
        for (int i = 0; i < count; i++) {
            Like like = new Like();
            like.setUserId(700000000L + i);
            like.setStickerSet(stickerSetRepository.findById(stickerSetId).orElseThrow());
            likeRepository.save(like);
        }
    }
}



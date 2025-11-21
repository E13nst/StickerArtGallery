package com.example.sticker_art_gallery.service;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("Лайки")
@Feature("Сервис лайков")
@DisplayName("Интеграционный тест LikeService (денормализованный likes_count)")
@Tag("integration")
class LikeServiceTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private StickerSetRepository stickerSetRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long userId;
    private Long stickerSetId;

    @BeforeEach
    void setUp() {
        userId = TestDataBuilder.TEST_USER_ID;
        StickerSet ss = new StickerSet();
        ss.setUserId(userId);
        ss.setTitle("Test-sticker");
        ss.setName("test_likes_service");
        ss.setState(StickerSetState.ACTIVE);
        ss.setVisibility(StickerSetVisibility.PUBLIC);
        ss.setType(StickerSetType.USER);
        StickerSet saved = stickerSetRepository.save(ss);
        stickerSetId = saved.getId();
    }

    @Test
    @Story("Инкремент/декремент likes_count при like/unlike")
    void likeAndUnlike_shouldUpdateLikesCount() {
        StickerSet before = stickerSetRepository.findById(stickerSetId).orElseThrow();
        int initial = before.getLikesCount();

        likeService.likeStickerSet(userId, stickerSetId);
        entityManager.flush();
        entityManager.clear();
        StickerSet afterLike = stickerSetRepository.findById(stickerSetId).orElseThrow();
        assertThat(afterLike.getLikesCount()).isEqualTo(initial + 1);

        likeService.unlikeStickerSet(userId, stickerSetId);
        entityManager.flush();
        entityManager.clear();
        StickerSet afterUnlike = stickerSetRepository.findById(stickerSetId).orElseThrow();
        assertThat(afterUnlike.getLikesCount()).isEqualTo(initial);
    }

    @Test
    @Story("Идемпотентность: повторный like не увеличивает счётчик")
    void duplicateLike_shouldNotIncreaseCount() {
        likeService.likeStickerSet(userId, stickerSetId);
        int afterFirst = stickerSetRepository.findById(stickerSetId).orElseThrow().getLikesCount();

        // Ожидаем IllegalArgumentException на повторном лайке
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            likeService.likeStickerSet(userId, stickerSetId)
        );

        int afterSecond = stickerSetRepository.findById(stickerSetId).orElseThrow().getLikesCount();
        assertThat(afterSecond).isEqualTo(afterFirst);
    }
}



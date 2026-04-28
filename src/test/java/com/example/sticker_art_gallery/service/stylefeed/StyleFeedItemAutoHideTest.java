package com.example.sticker_art_gallery.service.stylefeed;

import com.example.sticker_art_gallery.model.stylefeed.CandidateFeedVisibility;
import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест: автоскрытие при >= 7 дизлайках из > 10 голосов (CAS на БД).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Автоскрытие style feed item")
@Tag("integration")
class StyleFeedItemAutoHideTest {

    @Autowired
    private StyleFeedItemService styleFeedItemService;

    @Autowired
    private StyleFeedItemRepository styleFeedItemRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long itemId;

    @BeforeEach
    void setUp() {
        itemId = txTemplate.execute(status -> {
            Object rawId = entityManager.createNativeQuery(
                    "SELECT id FROM cached_images LIMIT 1").getSingleResult();
            UUID imageId = rawId instanceof UUID u ? u : UUID.fromString(rawId.toString());
            CachedImageEntity img = entityManager.find(CachedImageEntity.class, imageId);

            StyleFeedItemEntity item = new StyleFeedItemEntity();
            item.setTaskId("test-autohide-task-sf");
            item.setCachedImage(img);
            item.setLikesCount(4);
            item.setDislikesCount(0);
            return styleFeedItemRepository.save(item).getId();
        });
    }

    @Test
    @DisplayName("shouldAutoHideWhenDislikesReach7OutOf11Votes")
    @Transactional
    void shouldAutoHideWhenDislikesReach7OutOf11Votes() {
        for (int i = 1; i <= 6; i++) {
            long userId = 900_000_000L + i;
            styleFeedItemService.dislikeFeedItem(userId, itemId, false);
        }

        StyleFeedItemEntity afterSix = styleFeedItemRepository.findById(itemId).orElseThrow();
        assertThat(afterSix.getDislikesCount()).isEqualTo(6);
        assertThat(afterSix.getVisibility())
                .as("После 6 дизлайков из 10 голосов запись ещё видима")
                .isEqualTo(CandidateFeedVisibility.VISIBLE);

        styleFeedItemService.dislikeFeedItem(900_000_007L, itemId, false);

        StyleFeedItemEntity afterSeven = styleFeedItemRepository.findById(itemId).orElseThrow();
        assertThat(afterSeven.getDislikesCount()).isEqualTo(7);
        assertThat(afterSeven.getVisibility())
                .as("После 7 дизлайков из 11 голосов — AUTO_HIDDEN")
                .isEqualTo(CandidateFeedVisibility.AUTO_HIDDEN);
    }

    @Test
    @DisplayName("shouldNotAutoHideWhenAdminOverrideIsSet")
    @Transactional
    void shouldNotAutoHideWhenAdminOverrideIsSet() {
        txTemplate.execute(status -> {
            styleFeedItemRepository.setAdminVisibilityOverride(itemId, true);
            return null;
        });

        for (int i = 1; i <= 7; i++) {
            long userId = 800_000_000L + i;
            styleFeedItemService.dislikeFeedItem(userId, itemId, false);
        }

        StyleFeedItemEntity result = styleFeedItemRepository.findById(itemId).orElseThrow();
        assertThat(result.getVisibility())
                .as("При admin override=SHOW видимость ADMIN_FORCED_VISIBLE")
                .isEqualTo(CandidateFeedVisibility.ADMIN_FORCED_VISIBLE);
    }
}

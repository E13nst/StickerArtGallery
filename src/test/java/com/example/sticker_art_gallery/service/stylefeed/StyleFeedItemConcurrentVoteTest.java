package com.example.sticker_art_gallery.service.stylefeed;

import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemDislikeRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemLikeRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест: конкурентный like+dislike от одного пользователя.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Конкурентное голосование style feed item")
@Tag("integration")
class StyleFeedItemConcurrentVoteTest {

    @Autowired
    private StyleFeedItemService styleFeedItemService;

    @Autowired
    private StyleFeedItemLikeRepository likeRepository;

    @Autowired
    private StyleFeedItemDislikeRepository dislikeRepository;

    @Autowired
    private StyleFeedItemRepository styleFeedItemRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long itemId;
    private static final Long USER_ID = 999_000_001L;

    @BeforeEach
    void setUp() {
        itemId = txTemplate.execute(status -> {
            Object rawId = entityManager.createNativeQuery(
                    "SELECT id FROM cached_images LIMIT 1").getSingleResult();
            UUID imageId = rawId instanceof UUID u ? u : UUID.fromString(rawId.toString());

            CachedImageEntity img = entityManager.find(CachedImageEntity.class, imageId);
            StyleFeedItemEntity item = new StyleFeedItemEntity();
            item.setTaskId("test-concurrent-task-sf");
            item.setCachedImage(img);
            return styleFeedItemRepository.save(item).getId();
        });
    }

    @Test
    @DisplayName("shouldLeaveExactlyOneVoteWhenLikeAndDislikeConcurrent")
    void shouldLeaveExactlyOneVoteWhenLikeAndDislikeConcurrent() throws InterruptedException {
        int threads = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        pool.submit(() -> {
            try {
                startLatch.await();
                styleFeedItemService.likeFeedItem(USER_ID, itemId, false);
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        pool.submit(() -> {
            try {
                startLatch.await();
                styleFeedItemService.dislikeFeedItem(USER_ID, itemId, false);
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        pool.shutdown();

        long likesCount = likeRepository.findAll().stream()
                .filter(l -> USER_ID.equals(l.getUserId()) && itemId.equals(l.getStyleFeedItem().getId()))
                .count();
        long dislikesCount = dislikeRepository.findAll().stream()
                .filter(d -> USER_ID.equals(d.getUserId()) && itemId.equals(d.getStyleFeedItem().getId()))
                .count();

        assertThat(likesCount + dislikesCount)
                .as("Ровно одна оценка (лайк или дизлайк)")
                .isEqualTo(1);
    }
}

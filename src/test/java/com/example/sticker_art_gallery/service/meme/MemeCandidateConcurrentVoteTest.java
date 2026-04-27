package com.example.sticker_art_gallery.service.meme;

import com.example.sticker_art_gallery.model.meme.MemeCandidateEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateDislikeRepository;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateLikeRepository;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateRepository;
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
 * Два потока одновременно — должна остаться ровно одна запись (взаимоисключение).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Конкурентный like+dislike мем-кандидата")
@Tag("integration")
class MemeCandidateConcurrentVoteTest {

    @Autowired
    private MemeCandidateService memeCandidateService;

    @Autowired
    private MemeCandidateLikeRepository likeRepository;

    @Autowired
    private MemeCandidateDislikeRepository dislikeRepository;

    @Autowired
    private MemeCandidateRepository candidateRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long candidateId;
    private static final Long USER_ID = 999_000_001L;

    @BeforeEach
    void setUp() {
        // Создаём минимальный CachedImageEntity-заглушку через транзакцию
        candidateId = txTemplate.execute(status -> {
            // Ищем любое уже существующее cached_image для теста
            Object rawId = entityManager.createNativeQuery(
                    "SELECT id FROM cached_images LIMIT 1").getSingleResult();
            UUID imageId = rawId instanceof UUID u ? u : UUID.fromString(rawId.toString());

            CachedImageEntity img = entityManager.find(CachedImageEntity.class, imageId);
            MemeCandidateEntity candidate = new MemeCandidateEntity();
            candidate.setTaskId("test-concurrent-task");
            candidate.setCachedImage(img);
            return candidateRepository.save(candidate).getId();
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

        // Поток 1: лайк
        pool.submit(() -> {
            try {
                startLatch.await();
                memeCandidateService.likeCandidate(USER_ID, candidateId, false);
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Поток 2: дизлайк
        pool.submit(() -> {
            try {
                startLatch.await();
                memeCandidateService.dislikeCandidate(USER_ID, candidateId, false);
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // Стартуем оба потока одновременно
        doneLatch.await();
        pool.shutdown();

        // После конкурентного выполнения у пользователя должна остаться ровно одна запись
        long likesCount = likeRepository.findAll().stream()
                .filter(l -> USER_ID.equals(l.getUserId()) && candidateId.equals(l.getMemeCandidate().getId()))
                .count();
        long dislikesCount = dislikeRepository.findAll().stream()
                .filter(d -> USER_ID.equals(d.getUserId()) && candidateId.equals(d.getMemeCandidate().getId()))
                .count();

        assertThat(likesCount + dislikesCount)
                .as("Должна остаться ровно одна оценка (лайк или дизлайк)")
                .isEqualTo(1);
    }
}

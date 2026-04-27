package com.example.sticker_art_gallery.service.meme;

import com.example.sticker_art_gallery.model.meme.CandidateFeedVisibility;
import com.example.sticker_art_gallery.model.meme.MemeCandidateEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
 * Интеграционный тест: автоскрытие мем-кандидата при >= 7 дизлайках из > 10 голосов.
 * Проверяет атомарный CAS-апдейт на уровне БД (applyDislikeAndAutoHide).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Автоскрытие мем-кандидата")
@Tag("integration")
class MemeCandidateAutoHideTest {

    @Autowired
    private MemeCandidateService memeCandidateService;

    @Autowired
    private MemeCandidateRepository candidateRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long candidateId;

    @BeforeEach
    void setUp() {
        candidateId = txTemplate.execute(status -> {
            Long imageId = (Long) entityManager.createNativeQuery(
                    "SELECT id FROM cached_images LIMIT 1").getSingleResult();
            CachedImageEntity img = entityManager.find(CachedImageEntity.class, imageId);

            MemeCandidateEntity candidate = new MemeCandidateEntity();
            candidate.setTaskId("test-autohide-task");
            candidate.setCachedImage(img);
            candidate.setLikesCount(4);   // 4 лайка
            candidate.setDislikesCount(0); // 0 дизлайков начально
            return candidateRepository.save(candidate).getId();
        });
    }

    @Test
    @DisplayName("shouldAutoHideWhenDislikesReach7OutOf11Votes")
    @Transactional
    void shouldAutoHideWhenDislikesReach7OutOf11Votes() {
        // Уже 4 лайка. Добавляем 6 дизлайков (всего 10 голосов — порог не достигнут: > 10 голосов нужно)
        for (int i = 1; i <= 6; i++) {
            long userId = 900_000_000L + i;
            memeCandidateService.dislikeCandidate(userId, candidateId, false);
        }

        MemeCandidateEntity afterSix = candidateRepository.findById(candidateId).orElseThrow();
        assertThat(afterSix.getDislikesCount()).isEqualTo(6);
        assertThat(afterSix.getVisibility())
                .as("После 6 дизлайков из 10 голосов кандидат ещё видим")
                .isEqualTo(CandidateFeedVisibility.VISIBLE);

        // 7-й дизлайк: итого 7 из 11 голосов — условие AUTO_HIDDEN выполнено
        memeCandidateService.dislikeCandidate(900_000_007L, candidateId, false);

        MemeCandidateEntity afterSeven = candidateRepository.findById(candidateId).orElseThrow();
        assertThat(afterSeven.getDislikesCount()).isEqualTo(7);
        assertThat(afterSeven.getVisibility())
                .as("После 7 дизлайков из 11 голосов кандидат должен быть AUTO_HIDDEN")
                .isEqualTo(CandidateFeedVisibility.AUTO_HIDDEN);
    }

    @Test
    @DisplayName("shouldNotAutoHideWhenAdminOverrideIsSet")
    @Transactional
    void shouldNotAutoHideWhenAdminOverrideIsSet() {
        // Устанавливаем admin override SHOW
        txTemplate.execute(status -> {
            candidateRepository.setAdminVisibilityOverride(candidateId, true);
            return null;
        });

        // Добавляем 7 дизлайков из 11 голосов
        for (int i = 1; i <= 7; i++) {
            long userId = 800_000_000L + i;
            memeCandidateService.dislikeCandidate(userId, candidateId, false);
        }

        MemeCandidateEntity result = candidateRepository.findById(candidateId).orElseThrow();
        assertThat(result.getVisibility())
                .as("При admin override=SHOW видимость должна оставаться ADMIN_FORCED_VISIBLE")
                .isEqualTo(CandidateFeedVisibility.ADMIN_FORCED_VISIBLE);
    }
}

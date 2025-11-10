package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.model.profile.*;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Epic("ART-баллы")
@Feature("Сервис начислений")
@DisplayName("Интеграционный тест ArtRewardService")
@Tag("integration")
class ArtRewardServiceTest {

    private static final Long USER_ID = TestDataBuilder.TEST_USER_ID;
    private static final String TEST_METADATA = "{\"stickerSetId\": 777}";
    private static final String TEST_EXTERNAL_ID = "it:art:upload:777";

    @Autowired
    private ArtRewardService artRewardService;

    @Autowired
    private ArtRuleService artRuleService;

    @Autowired
    private ArtRuleRepository artRuleRepository;

    @Autowired
    private ArtTransactionRepository artTransactionRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Убеждаемся, что профиль пользователя существует
        userProfileRepository.findByUserId(USER_ID).orElseGet(() -> {
            UserProfileEntity profile = new UserProfileEntity();
            profile.setUserId(USER_ID);
            profile.setRole(UserProfileEntity.UserRole.USER);
            profile.setArtBalance(0L);
            profile.setIsBlocked(false);
            return userProfileRepository.save(profile);
        });

        // Убеждаемся, что базовое правило начисления доступно
        if (artRuleRepository.findByCode(ArtRewardService.RULE_UPLOAD_STICKERSET).isEmpty()) {
            ArtRuleEntity rule = new ArtRuleEntity();
            rule.setCode(ArtRewardService.RULE_UPLOAD_STICKERSET);
            rule.setDirection(ArtTransactionDirection.CREDIT);
            rule.setAmount(10L);
            rule.setIsEnabled(Boolean.TRUE);
            rule.setDescription("Начисление за тестовую загрузку стикерсета");
            artRuleService.save(rule);
        }
    }

    @Test
    @Story("Начисление ART по правилу UPLOAD_STICKERSET")
    void awardUploadStickerSet_shouldIncreaseBalanceAndPersistTransaction() {
        ArtTransactionEntity transaction = artRewardService.award(
                USER_ID,
                ArtRewardService.RULE_UPLOAD_STICKERSET,
                null,
                TEST_METADATA,
                TEST_EXTERNAL_ID,
                USER_ID
        );

        entityManager.flush();
        entityManager.clear();

        Optional<ArtTransactionEntity> savedTx = artTransactionRepository.findByExternalId(TEST_EXTERNAL_ID);
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();

        assertThat(savedTx).isPresent();
        assertThat(savedTx.get().getRuleCode()).isEqualTo(ArtRewardService.RULE_UPLOAD_STICKERSET);
        assertThat(savedTx.get().getDelta()).isEqualTo(10L);
        assertThat(savedTx.get().getBalanceAfter()).isEqualTo(profile.getArtBalance());
        assertThat(profile.getArtBalance()).isEqualTo(10L);
        assertThat(transaction.getMetadata()).isEqualTo(TEST_METADATA);
    }

    @Test
    @Story("Идемпотентность начисления по externalId")
    void award_withSameExternalId_shouldReturnExistingTransaction() {
        ArtTransactionEntity first = artRewardService.award(
                USER_ID,
                ArtRewardService.RULE_UPLOAD_STICKERSET,
                null,
                TEST_METADATA,
                TEST_EXTERNAL_ID,
                USER_ID
        );

        ArtTransactionEntity second = artRewardService.award(
                USER_ID,
                ArtRewardService.RULE_UPLOAD_STICKERSET,
                null,
                TEST_METADATA,
                TEST_EXTERNAL_ID,
                USER_ID
        );

        entityManager.flush();
        entityManager.clear();

        long txCount = artTransactionRepository.count();
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(txCount).isEqualTo(1L);
        assertThat(profile.getArtBalance()).isEqualTo(10L);
    }

    @Test
    @Story("Списание ART по правилу DEBIT")
    void award_debitRule_shouldDecreaseBalance() {
        // Подготавливаем баланс: +20 ART
        ArtRuleEntity creditRule = artRuleRepository.findByCode(ArtRewardService.RULE_UPLOAD_STICKERSET).orElseThrow();
        artRewardService.award(USER_ID, creditRule.getCode(), 20L, null, "it:art:credit:bootstrap", USER_ID);

        ArtRuleEntity debitRule = artRuleRepository.findByCode("TEST_DEBIT")
                .orElseGet(() -> {
                    ArtRuleEntity rule = new ArtRuleEntity();
                    rule.setCode("TEST_DEBIT");
                    rule.setDirection(ArtTransactionDirection.DEBIT);
                    rule.setAmount(5L);
                    rule.setIsEnabled(Boolean.TRUE);
                    rule.setDescription("Тестовое списание");
                    return artRuleService.save(rule);
                });

        ArtTransactionEntity debitTx = artRewardService.award(
                USER_ID,
                debitRule.getCode(),
                null,
                null,
                "it:art:debit:test",
                999L
        );

        entityManager.flush();
        entityManager.clear();

        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(profile.getArtBalance()).isEqualTo(15L);
        assertThat(debitTx.getDirection()).isEqualTo(ArtTransactionDirection.DEBIT);
        assertThat(debitTx.getDelta()).isEqualTo(-5L);
        assertThat(debitTx.getPerformedBy()).isEqualTo(999L);
    }
}


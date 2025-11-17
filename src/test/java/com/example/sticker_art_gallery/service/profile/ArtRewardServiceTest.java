package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.model.profile.*;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
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
        // Получаем начальный баланс
        UserProfileEntity initialProfile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long initialBalance = initialProfile.getArtBalance();

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
        // Проверяем относительное изменение баланса
        assertThat(profile.getArtBalance()).isEqualTo(initialBalance + 10L);
        assertThat(transaction.getMetadata()).isEqualTo(TEST_METADATA);
    }

    @Test
    @Story("Идемпотентность начисления по externalId")
    void award_withSameExternalId_shouldReturnExistingTransaction() {
        // Получаем начальный баланс
        UserProfileEntity initialProfile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long initialBalance = initialProfile.getArtBalance();

        ArtTransactionEntity first = artRewardService.award(
                USER_ID,
                ArtRewardService.RULE_UPLOAD_STICKERSET,
                null,
                TEST_METADATA,
                TEST_EXTERNAL_ID,
                USER_ID
        );

        // Получаем баланс после первого начисления
        entityManager.flush();
        entityManager.clear();
        UserProfileEntity afterFirst = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long balanceAfterFirst = afterFirst.getArtBalance();

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

        // Проверяем, что транзакция с нашим externalId существует и единственная
        Optional<ArtTransactionEntity> txByExternalId = artTransactionRepository.findByExternalId(TEST_EXTERNAL_ID);
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(txByExternalId).isPresent();
        assertThat(txByExternalId.get().getId()).isEqualTo(first.getId());
        // Проверяем, что баланс увеличился только один раз (после первого вызова)
        assertThat(balanceAfterFirst).isEqualTo(initialBalance + 10L);
        assertThat(profile.getArtBalance()).isEqualTo(balanceAfterFirst); // Баланс не изменился после второго вызова
    }

    @Test
    @Story("Списание ART по правилу DEBIT")
    void award_debitRule_shouldDecreaseBalance() {
        // Получаем начальный баланс
        UserProfileEntity initialProfile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long initialBalance = initialProfile.getArtBalance();

        // Подготавливаем баланс: +20 ART
        ArtRuleEntity creditRule = artRuleRepository.findByCode(ArtRewardService.RULE_UPLOAD_STICKERSET).orElseThrow();
        artRewardService.award(USER_ID, creditRule.getCode(), 20L, null, "it:art:credit:bootstrap", USER_ID);

        // Получаем баланс после начисления
        entityManager.flush();
        entityManager.clear();
        UserProfileEntity afterCredit = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long balanceAfterCredit = afterCredit.getArtBalance();

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
        // Проверяем относительное изменение: начальный + 20 - 5 = начальный + 15
        assertThat(balanceAfterCredit).isEqualTo(initialBalance + 20L);
        assertThat(profile.getArtBalance()).isEqualTo(initialBalance + 15L);
        assertThat(debitTx.getDirection()).isEqualTo(ArtTransactionDirection.DEBIT);
        assertThat(debitTx.getDelta()).isEqualTo(-5L);
        assertThat(debitTx.getPerformedBy()).isEqualTo(999L);
    }
}


package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.model.profile.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ArtRewardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtRewardService.class);

    public static final String RULE_UPLOAD_STICKERSET = "UPLOAD_STICKERSET";
    public static final String RULE_PUBLISH_STICKERSET = "PUBLISH_STICKERSET";
    public static final String RULE_GENERATE_STICKER = "GENERATE_STICKER";

    private final UserProfileService userProfileService;
    private final ArtRuleService artRuleService;
    private final ArtTransactionRepository artTransactionRepository;

    public ArtRewardService(UserProfileService userProfileService,
                            ArtRuleService artRuleService,
                            ArtTransactionRepository artTransactionRepository) {
        this.userProfileService = userProfileService;
        this.artRuleService = artRuleService;
        this.artTransactionRepository = artTransactionRepository;
    }

    public ArtTransactionEntity award(Long userId,
                                      String ruleCode,
                                      Long overrideAmount,
                                      String metadata,
                                      String externalId,
                                      Long performedBy) {
        if (userId == null) {
            throw new IllegalArgumentException("userId обязателен для начисления ART");
        }
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new IllegalArgumentException("ruleCode обязателен для начисления ART");
        }

        if (externalId != null) {
            Optional<ArtTransactionEntity> existing = artTransactionRepository.findByExternalId(externalId);
            if (existing.isPresent()) {
                LOGGER.info("♻️ Пропускаем повторное начисление по externalId={} ruleCode={}", externalId, ruleCode);
                return existing.get();
            }
        }

        ArtRuleEntity rule = artRuleService.getEnabledRuleOrThrow(ruleCode);

        long amount = overrideAmount != null ? overrideAmount : rule.getAmount();
        long delta = rule.getDirection().apply(amount);

        if (delta == 0) {
            throw new IllegalStateException("Сумма транзакции должна быть ненулевой");
        }

        UserProfileEntity profile = userProfileService.getOrCreateDefaultForUpdate(userId);
        long newBalance = profile.getArtBalance() + delta;
        if (newBalance < 0) {
            throw new IllegalStateException(String.format(
                    "Недостаточно ART: текущий баланс=%d, delta=%d, userId=%d",
                    profile.getArtBalance(), delta, userId
            ));
        }

        profile.setArtBalance(newBalance);

        ArtTransactionEntity transaction = new ArtTransactionEntity();
        transaction.setUserProfile(profile);
        transaction.setUserId(userId);
        transaction.setRule(rule);
        transaction.setRuleCode(rule.getCode());
        transaction.setDirection(rule.getDirection());
        transaction.setDelta(delta);
        transaction.setBalanceAfter(newBalance);
        transaction.setMetadata(metadata);
        transaction.setExternalId(externalId);
        transaction.setPerformedBy(performedBy);

        ArtTransactionEntity saved = artTransactionRepository.save(transaction);
        LOGGER.info("💠 ART транзакция: userId={}, rule={}, delta={}, balanceAfter={}",
                userId, ruleCode, delta, newBalance);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ArtTransactionEntity> findTransactions(Long userId, Pageable pageable) {
        return artTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

}


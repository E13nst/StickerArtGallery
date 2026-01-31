package com.example.sticker_art_gallery.service.referral;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.referral.ReferralLinkDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.referral.ReferralCodeEntity;
import com.example.sticker_art_gallery.model.referral.ReferralEntity;
import com.example.sticker_art_gallery.model.referral.ReferralEventEntity;
import com.example.sticker_art_gallery.repository.ReferralCodeRepository;
import com.example.sticker_art_gallery.repository.ReferralEventRepository;
import com.example.sticker_art_gallery.repository.ReferralRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class ReferralService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferralService.class);
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private static final String EVENT_TYPE_INVITEE_BONUS = "INVITEE_BONUS_GRANTED";
    private static final String EVENT_TYPE_FIRST_GENERATION = "FIRST_GENERATION_REWARD_GRANTED";
    
    private static final String RULE_CODE_INVITEE_BONUS = "REFERRAL_INVITEE_BONUS";
    private static final String RULE_CODE_REFERRER_FIRST_GENERATION = "REFERRAL_REFERRER_FIRST_GENERATION";

    private final ReferralCodeRepository referralCodeRepository;
    private final ReferralRepository referralRepository;
    private final ReferralEventRepository referralEventRepository;
    private final ArtRewardService artRewardService;
    private final AppConfig appConfig;

    @Autowired
    public ReferralService(ReferralCodeRepository referralCodeRepository,
                          ReferralRepository referralRepository,
                          ReferralEventRepository referralEventRepository,
                          ArtRewardService artRewardService,
                          AppConfig appConfig) {
        this.referralCodeRepository = referralCodeRepository;
        this.referralRepository = referralRepository;
        this.referralEventRepository = referralEventRepository;
        this.artRewardService = artRewardService;
        this.appConfig = appConfig;
    }

    /**
     * Получить или создать реферальную ссылку для пользователя
     */
    public ReferralLinkDto getOrCreateMyReferralLink(Long userId) {
        LOGGER.debug("🔗 Получение реферальной ссылки для пользователя: {}", userId);
        
        // Проверяем существующий код
        Optional<ReferralCodeEntity> existingCode = referralCodeRepository.findById(userId);
        
        ReferralCodeEntity codeEntity;
        if (existingCode.isPresent()) {
            codeEntity = existingCode.get();
            LOGGER.debug("✅ Найден существующий код: {}", codeEntity.getCode());
        } else {
            // Генерируем новый код
            codeEntity = new ReferralCodeEntity();
            codeEntity.setUserId(userId);
            codeEntity.setCode(generateUniqueCode());
            codeEntity = referralCodeRepository.save(codeEntity);
            LOGGER.info("✅ Создан новый реферальный код для пользователя {}: {}", userId, codeEntity.getCode());
        }
        
        String code = codeEntity.getCode();
        String startParam = "ref_" + code;
        String botUsername = appConfig.getTelegram().getBotUsername();
        String url = String.format("https://t.me/%s?startapp=%s", botUsername, startParam);
        
        return new ReferralLinkDto(code, startParam, url);
    }

    /**
     * Обработка первой аутентификации пользователя с реферальным параметром
     */
    public void onFirstAuthentication(Long userId, String startParam, String metadataJson) {
        if (startParam == null || startParam.isEmpty()) {
            LOGGER.debug("⏭️ Нет startParam, пропускаем реферальную обработку для пользователя {}", userId);
            return;
        }
        
        // Валидация формата
        if (!startParam.startsWith("ref_") || startParam.length() < 8) {
            LOGGER.debug("⏭️ Невалидный формат startParam: {}, пропускаем", startParam);
            return;
        }
        
        // Проверяем, не атрибутирован ли уже пользователь
        if (referralRepository.existsByReferredUserId(userId)) {
            LOGGER.debug("⏭️ Пользователь {} уже атрибутирован, пропускаем", userId);
            return;
        }
        
        String code = startParam.substring(4);
        LOGGER.info("🎁 Обработка реферальной атрибуции: userId={}, code={}", userId, code);
        
        // Находим реферера по коду
        Optional<ReferralCodeEntity> referralCodeOpt = referralCodeRepository.findByCode(code);
        if (referralCodeOpt.isEmpty()) {
            LOGGER.warn("⚠️ Реферальный код не найден: {}", code);
            return;
        }
        
        Long referrerUserId = referralCodeOpt.get().getUserId();
        
        // Self-referral защита
        if (referrerUserId.equals(userId)) {
            LOGGER.warn("⚠️ Попытка self-referral: userId={}", userId);
            return;
        }
        
        try {
            // Создаём запись атрибуции
            ReferralEntity referral = new ReferralEntity();
            referral.setReferrerUserId(referrerUserId);
            referral.setReferredUserId(userId);
            referral.setStartParam(startParam);
            referral.setMetadata(metadataJson);
            referral.setStatus("ACTIVE");
            referral = referralRepository.save(referral);
            
            LOGGER.info("✅ Создана реферальная связь: referrer={} → referred={}, id={}", 
                    referrerUserId, userId, referral.getId());
            
            // Начисляем бонус приглашённому
            awardInviteeBonus(referral, userId);
            
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("⚠️ Конфликт при создании referral (уже существует): userId={}", userId);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка обработки реферальной атрибуции для userId={}: {}", userId, e.getMessage(), e);
        }
    }

    /**
        if (startParam == null || startParam.isEmpty()) {
            LOGGER.debug("⏭️ Нет startParam, пропускаем реферальную обработку для пользователя {}", userId);
            return;
        }
        
        // Валидация формата
        if (!startParam.startsWith("ref_") || startParam.length() < 8) {
            LOGGER.debug("⏭️ Невалидный формат startParam: {}, пропускаем", startParam);
            return;
        }
        
        // Проверяем, не атрибутирован ли уже пользователь
        if (referralRepository.existsByReferredUserId(userId)) {
            LOGGER.debug("⏭️ Пользователь {} уже атрибутирован, пропускаем", userId);
            return;
        }
        
        String code = startParam.substring(4);
        LOGGER.info("🎁 Обработка реферальной атрибуции: userId={}, code={}", userId, code);
        
        // Находим реферера по коду
        Optional<ReferralCodeEntity> referralCodeOpt = referralCodeRepository.findByCode(code);
        if (referralCodeOpt.isEmpty()) {
            LOGGER.warn("⚠️ Реферальный код не найден: {}", code);
            return;
        }
        
        Long referrerUserId = referralCodeOpt.get().getUserId();
        
        // Self-referral защита
        if (referrerUserId.equals(userId)) {
            LOGGER.warn("⚠️ Попытка self-referral: userId={}", userId);
            return;
        }
        
        try {
            // Создаём запись атрибуции
            ReferralEntity referral = new ReferralEntity();
            referral.setReferrerUserId(referrerUserId);
            referral.setReferredUserId(userId);
            referral.setStartParam(startParam);
            referral.setMetadata(metadataJson);
            referral.setStatus("ACTIVE");
            referral = referralRepository.save(referral);
            
            LOGGER.info("✅ Создана реферальная связь: referrer={} → referred={}, id={}", 
                    referrerUserId, userId, referral.getId());
            
            // Начисляем бонус приглашённому
            awardInviteeBonus(referral, userId);
            
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("⚠️ Конфликт при создании referral (уже существует): userId={}", userId);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка обработки реферальной атрибуции для userId={}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Обработка первой генерации приглашённым пользователем
     */
    public void onFirstGeneration(Long referredUserId, String generationTaskId) {
        LOGGER.debug("🎨 Проверка реферального бонуса за первую генерацию: userId={}, taskId={}", 
                referredUserId, generationTaskId);
        
        // Проверяем наличие реферальной связи
        Optional<ReferralEntity> referralOpt = referralRepository.findByReferredUserId(referredUserId);
        if (referralOpt.isEmpty()) {
            LOGGER.debug("⏭️ Нет реферальной связи для пользователя {}", referredUserId);
            return;
        }
        
        ReferralEntity referral = referralOpt.get();
        
        // Проверяем, не начислена ли уже награда
        if (referral.getReferrerFirstGenerationAwardedAt() != null) {
            LOGGER.debug("⏭️ Награда за первую генерацию уже начислена рефереру {}", referral.getReferrerUserId());
            return;
        }
        
        Long referrerUserId = referral.getReferrerUserId();
        
        // Проверяем дневной cap
        int dailyCap = appConfig.getReferral().getDailyReferrerRewardCap();
        OffsetDateTime dayAgo = OffsetDateTime.now().minusDays(1);
        long rewardsLast24h = referralRepository.countByReferrerUserIdAndReferrerFirstGenerationAwardedAtAfter(
                referrerUserId, dayAgo);
        
        if (rewardsLast24h >= dailyCap) {
            LOGGER.warn("⚠️ Превышен дневной cap ({}) для реферера {}: {} наград за 24ч", 
                    dailyCap, referrerUserId, rewardsLast24h);
            return;
        }
        
        // Проверяем окно атрибуции (опционально)
        int windowDays = appConfig.getReferral().getAttributionWindowDays();
        OffsetDateTime windowStart = OffsetDateTime.now().minusDays(windowDays);
        if (referral.getCreatedAt().isBefore(windowStart)) {
            LOGGER.warn("⚠️ Реферальная связь {} вне окна атрибуции ({} дней)", referral.getId(), windowDays);
            return;
        }
        
        try {
            // Начисляем бонус рефереру
            String externalId = "referral:first_generation:" + referredUserId;
            String metadata = String.format("{\"referredUserId\":%d,\"taskId\":\"%s\"}", 
                    referredUserId, generationTaskId);
            
            ArtTransactionEntity transaction = artRewardService.award(
                    referrerUserId,
                    RULE_CODE_REFERRER_FIRST_GENERATION,
                    null,
                    metadata,
                    externalId,
                    null
            );
            
            // Обновляем referral
            referral.setReferrerFirstGenerationAwardedAt(OffsetDateTime.now());
            referralRepository.save(referral);
            
            // Создаём event для аудита
            createReferralEvent(referral, EVENT_TYPE_FIRST_GENERATION, transaction, externalId);
            
            LOGGER.info("✅ Начислен бонус +50 ART рефереру {} за первую генерацию приглашённого {}", 
                    referrerUserId, referredUserId);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка начисления бонуса рефереру {} за первую генерацию {}: {}", 
                    referrerUserId, referredUserId, e.getMessage(), e);
        }
    }

    /**
     * Начислить бонус приглашённому пользователю
     */
    private void awardInviteeBonus(ReferralEntity referral, Long userId) {
        try {
            String externalId = "referral:invitee_bonus:" + userId;
            String metadata = String.format("{\"referrerUserId\":%d}", referral.getReferrerUserId());
            
            ArtTransactionEntity transaction = artRewardService.award(
                    userId,
                    RULE_CODE_INVITEE_BONUS,
                    null,
                    metadata,
                    externalId,
                    null
            );
            
            // Обновляем referral
            referral.setInviteeBonusAwardedAt(OffsetDateTime.now());
            referralRepository.save(referral);
            
            // Создаём event для аудита
            createReferralEvent(referral, EVENT_TYPE_INVITEE_BONUS, transaction, externalId);
            
            LOGGER.info("✅ Начислен бонус +100 ART приглашённому {}", userId);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка начисления бонуса приглашённому {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Создать запись события для аудита
     */
    private void createReferralEvent(ReferralEntity referral, String eventType, 
                                    ArtTransactionEntity transaction, String externalId) {
        try {
            ReferralEventEntity event = new ReferralEventEntity();
            event.setReferral(referral);
            event.setEventType(eventType);
            event.setArtTransaction(transaction);
            event.setExternalId(externalId);
            referralEventRepository.save(event);
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось создать referral_event: {}", e.getMessage());
        }
    }

    /**
     * Сгенерировать уникальный код
     */
    private String generateUniqueCode() {
        int codeLength = appConfig.getReferral().getCodeLength();
        int maxAttempts = 10;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String code = generateRandomBase62(codeLength);
            if (!referralCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        
        throw new IllegalStateException("Не удалось сгенерировать уникальный реферальный код после " + maxAttempts + " попыток");
    }

    /**
     * Сгенерировать случайную строку base62
     */
    private String generateRandomBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(RANDOM.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }
}

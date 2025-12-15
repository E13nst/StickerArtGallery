package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.exception.StickerSetNotFoundException;
import com.example.sticker_art_gallery.exception.WalletNotFoundException;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.transaction.*;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с намерениями транзакций
 * 
 * Правило 1: Всегда создавать leg при создании intent
 * Правило 2: Intent всегда первичен (создается первым, затем legs в одной транзакции)
 */
@Service
@Transactional
public class TransactionIntentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionIntentService.class);

    private final TransactionIntentRepository intentRepository;
    private final TransactionLegRepository legRepository;
    private final UserRepository userRepository;
    private final PlatformEntityService platformEntityService;
    private final StickerSetRepository stickerSetRepository;
    private final WalletService walletService;

    public TransactionIntentService(
            TransactionIntentRepository intentRepository,
            TransactionLegRepository legRepository,
            UserRepository userRepository,
            PlatformEntityService platformEntityService,
            StickerSetRepository stickerSetRepository,
            WalletService walletService) {
        this.intentRepository = intentRepository;
        this.legRepository = legRepository;
        this.userRepository = userRepository;
        this.platformEntityService = platformEntityService;
        this.stickerSetRepository = stickerSetRepository;
        this.walletService = walletService;
    }

    /**
     * Создать намерение транзакции с обязательным созданием хотя бы одного MAIN leg
     * 
     * Правило 1: Всегда создавать leg
     * Правило 2: Intent создается первым, затем Legs в одной транзакции
     */
    public TransactionIntentEntity createIntent(
            IntentType type,
            Long userId,
            Long subjectEntityId,
            Long amountNano,
            Map<String, Object> metadata) {
        
        LOGGER.info("Создание намерения транзакции: type={}, userId={}, subjectEntityId={}, amountNano={}", 
                   type, userId, subjectEntityId, amountNano);

        // Получаем пользователя
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
        }
        UserEntity user = userOpt.get();

        // Получаем или создаем subject entity
        PlatformEntityEntity subjectEntity = null;
        if (subjectEntityId != null) {
            Optional<PlatformEntityEntity> subjectOpt = platformEntityService.findByTypeAndRef(
                    EntityType.STICKER_SET, "sticker_set:" + subjectEntityId);
            if (subjectOpt.isEmpty()) {
                // Создаем entity автоматически
                subjectEntity = platformEntityService.getOrCreate(
                        EntityType.STICKER_SET, 
                        "sticker_set:" + subjectEntityId, 
                        null);
            } else {
                subjectEntity = subjectOpt.get();
            }
        }

        // Правило 2: Создаем Intent первым
        TransactionIntentEntity intent = new TransactionIntentEntity();
        intent.setIntentType(type);
        intent.setUser(user);
        intent.setSubjectEntity(subjectEntity);
        intent.setAmountNano(amountNano);
        intent.setCurrency("TON");
        intent.setStatus(IntentStatus.CREATED);
        
        // Сохраняем metadata как JSON строку (если нужно, можно использовать Jackson для сериализации)
        if (metadata != null && !metadata.isEmpty()) {
            // Простая сериализация для примера (в production лучше использовать Jackson)
            intent.setMetadata(metadata.toString());
        }

        intent = intentRepository.save(intent);
        LOGGER.debug("Intent создан: id={}", intent.getId());

        // Правило 1: Обязательно создаем MAIN leg
        TransactionLegEntity mainLeg = new TransactionLegEntity();
        mainLeg.setIntent(intent);
        mainLeg.setLegType(LegType.MAIN);
        mainLeg.setToEntity(subjectEntity);
        
        // Получаем адрес кошелька получателя из subject entity
        // Для упрощения: если subjectEntity - это STICKER_SET, нужно получить кошелек автора
        // Пока используем заглушку - в реальности нужно получать из UserWallet
        String toWalletAddress = getWalletAddressForEntity(subjectEntity);
        mainLeg.setToWalletAddress(toWalletAddress);
        mainLeg.setAmountNano(amountNano);

        legRepository.save(mainLeg);
        LOGGER.debug("MAIN leg создан для intent: id={}", intent.getId());

        // Если создание leg не удалось, транзакция откатится автоматически (Правило 1)
        
        return intent;
    }

    /**
     * Получить адрес кошелька для сущности
     * TODO: Реализовать логику получения кошелька в зависимости от типа сущности
     */
    private String getWalletAddressForEntity(PlatformEntityEntity entity) {
        if (entity == null) {
            throw new IllegalStateException("Subject entity не может быть null для MAIN leg");
        }
        
        // Заглушка: в реальности нужно получать кошелек из UserWallet
        // Для STICKER_SET - кошелек автора
        // Для USER - кошелек пользователя
        // Для PLATFORM - платформенный кошелек
        return "EQDummyWalletAddressForEntity_" + entity.getId();
    }

    /**
     * Обновить статус намерения
     */
    public TransactionIntentEntity updateStatus(Long intentId, IntentStatus status) {
        LOGGER.info("Обновление статуса намерения: intentId={}, newStatus={}", intentId, status);
        
        Optional<TransactionIntentEntity> intentOpt = intentRepository.findById(intentId);
        if (intentOpt.isEmpty()) {
            throw new IllegalArgumentException("Намерение с ID " + intentId + " не найдено");
        }

        TransactionIntentEntity intent = intentOpt.get();
        intent.setStatus(status);
        return intentRepository.save(intent);
    }

    /**
     * Найти намерение по ID
     */
    @Transactional(readOnly = true)
    public Optional<TransactionIntentEntity> findById(Long intentId) {
        return intentRepository.findById(intentId);
    }

    /**
     * Получить все legs для намерения
     */
    @Transactional(readOnly = true)
    public List<TransactionLegEntity> getLegsForIntent(Long intentId) {
        return legRepository.findByIntent_Id(intentId);
    }

    /**
     * Создать намерение доната для стикерсета
     * 
     * @param donorUserId ID пользователя-донатора
     * @param stickerSetId ID стикерсета
     * @param amountNano сумма доната в нано-TON
     * @return созданное намерение транзакции
     * @throws StickerSetNotFoundException если стикерсет не найден
     * @throws IllegalStateException если стикерсет не имеет автора
     * @throws WalletNotFoundException если у автора нет привязанного кошелька
     */
    public TransactionIntentEntity createStickerSetDonationIntent(
            Long donorUserId,
            Long stickerSetId,
            Long amountNano) {
        
        LOGGER.info("Создание donation intent: donorUserId={}, stickerSetId={}, amountNano={}", 
                   donorUserId, stickerSetId, amountNano);

        // 1. Найти StickerSet
        Optional<StickerSet> stickerSetOpt = stickerSetRepository.findById(stickerSetId);
        if (stickerSetOpt.isEmpty()) {
            throw new StickerSetNotFoundException(stickerSetId);
        }
        StickerSet stickerSet = stickerSetOpt.get();

        // 2. Проверить автора
        Long authorId = stickerSet.getAuthorId();
        if (authorId == null) {
            throw new IllegalStateException("StickerSet не имеет автора");
        }

        // 3. Найти кошелёк автора
        UserWalletEntity authorWallet = walletService.getActiveWallet(authorId);
        String authorWalletAddress = authorWallet.getWalletAddress();

        // 4. Создать PlatformEntity для StickerSet
        PlatformEntityEntity stickerSetEntity = platformEntityService.getOrCreate(
                EntityType.STICKER_SET,
                "sticker_set:" + stickerSetId,
                authorId
        );

        // 5. Создать или получить PlatformEntity для автора
        PlatformEntityEntity authorEntity = platformEntityService.getOrCreate(
                EntityType.USER,
                "user:" + authorId,
                authorId
        );

        // 6. Получить донатора
        Optional<UserEntity> donorOpt = userRepository.findById(donorUserId);
        if (donorOpt.isEmpty()) {
            throw new IllegalArgumentException("Пользователь-донатор с ID " + donorUserId + " не найден");
        }
        UserEntity donor = donorOpt.get();

        // 7. Создать TransactionIntent
        TransactionIntentEntity intent = new TransactionIntentEntity();
        intent.setIntentType(IntentType.DONATION);
        intent.setUser(donor);
        intent.setSubjectEntity(stickerSetEntity);
        intent.setStatus(IntentStatus.CREATED);
        intent.setAmountNano(amountNano);
        intent.setCurrency("TON");

        intent = intentRepository.save(intent);
        LOGGER.debug("Intent создан: id={}", intent.getId());

        // 8. Создать ОДИН TransactionLeg
        TransactionLegEntity mainLeg = new TransactionLegEntity();
        mainLeg.setIntent(intent);
        mainLeg.setLegType(LegType.MAIN);
        mainLeg.setToEntity(authorEntity);
        mainLeg.setToWalletAddress(authorWalletAddress);
        mainLeg.setAmountNano(amountNano);

        legRepository.save(mainLeg);
        LOGGER.debug("MAIN leg создан для intent: id={}, toWallet={}", intent.getId(), authorWalletAddress);

        // 9. Проверка инварианта: сумма всех legs должна быть равна intent.amountNano
        // Перезагружаем legs из БД, так как они могут быть не загружены из-за LAZY loading
        List<TransactionLegEntity> savedLegs = legRepository.findByIntent_Id(intent.getId());
        long totalLegsAmount = savedLegs.stream()
                .mapToLong(TransactionLegEntity::getAmountNano)
                .sum();
        
        if (totalLegsAmount != intent.getAmountNano()) {
            throw new IllegalStateException(
                    "Intent amount mismatch: intent=" + intent.getAmountNano() + 
                    ", legs total=" + totalLegsAmount
            );
        }

        return intent;
    }
}


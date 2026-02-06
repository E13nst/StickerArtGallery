package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.dto.payment.*;
import com.example.sticker_art_gallery.model.payment.*;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.*;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для обработки платежей Telegram Stars
 */
@Service
@Transactional
public class StarsPaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarsPaymentService.class);
    private static final String ART_RULE_PURCHASE_STARS = "PURCHASE_STARS";

    private final StarsPackageRepository starsPackageRepository;
    private final StarsPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ArtRewardService artRewardService;

    public StarsPaymentService(StarsPackageRepository starsPackageRepository,
                               StarsPurchaseRepository purchaseRepository,
                               UserRepository userRepository,
                               ArtRewardService artRewardService) {
        this.starsPackageRepository = starsPackageRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.artRewardService = artRewardService;
    }

    /**
     * Получить список активных тарифных пакетов
     */
    @Transactional(readOnly = true)
    public List<StarsPackageDto> getActivePackages() {
        List<StarsPackageEntity> packages = starsPackageRepository.findByIsEnabledTrueOrderBySortOrder();
        return packages.stream()
                .map(StarsPackageDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить историю покупок пользователя
     */
    @Transactional(readOnly = true)
    public Page<StarsPurchaseDto> getPurchaseHistory(Long userId, Pageable pageable) {
        Page<StarsPurchaseEntity> purchases = purchaseRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return purchases.map(StarsPurchaseDto::fromEntity);
    }

    /**
     * Получить последнюю покупку пользователя
     */
    @Transactional(readOnly = true)
    public Optional<StarsPurchaseDto> getRecentPurchase(Long userId) {
        List<StarsPurchaseEntity> purchases = purchaseRepository.findByUserIdOrderByCreatedAtDesc(userId, 
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent();
        
        if (purchases.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(StarsPurchaseDto.fromEntity(purchases.get(0)));
    }

    /**
     * Обработка webhook платежа от Python сервиса
     * (после successful_payment от Telegram)
     */
    public ProcessPaymentResponse processWebhookPayment(TelegramWebhookRequest request) {
        LOGGER.info("💰 Обработка webhook платежа: chargeId={}, userId={}, amountStars={}, payload={}",
                request.getTelegramChargeId(), request.getUserId(), request.getAmountStars(), request.getInvoicePayload());

        // 1. Проверка идемпотентности по telegram_charge_id
        Optional<StarsPurchaseEntity> existingPurchase = purchaseRepository.findByTelegramChargeId(request.getTelegramChargeId());
        if (existingPurchase.isPresent()) {
            LOGGER.info("♻️ Платеж уже обработан (идемпотентность): purchaseId={}", existingPurchase.get().getId());
            StarsPurchaseEntity purchase = existingPurchase.get();
            return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
        }

        // 2. Парсинг package_id из invoice_payload
        Long packageId = request.getPackageIdFromPayload();
        if (packageId == null) {
            String errorMsg = "Не удалось извлечь package_id из invoice_payload: " + request.getInvoicePayload();
            LOGGER.error("❌ {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        LOGGER.debug("🔍 Извлечен package_id: {}", packageId);

        // 3. Поиск пакета по ID
        StarsPackageEntity starsPackage = starsPackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Пакет не найден: " + packageId));

        // Проверка что пакет активен
        if (!starsPackage.getIsEnabled()) {
            String errorMsg = "Пакет отключен: " + packageId;
            LOGGER.error("❌ {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // 4. Валидация суммы
        if (!starsPackage.getStarsPrice().equals(request.getAmountStars())) {
            String errorMsg = String.format("Несоответствие суммы: ожидалось %d Stars, получено %d Stars",
                    starsPackage.getStarsPrice(), request.getAmountStars());
            LOGGER.error("❌ {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // 5. Поиск пользователя
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + request.getUserId()));

        // 6. Начисление ART через ArtRewardService
        String metadata = String.format("{\"packageCode\":\"%s\",\"packageId\":%d,\"starsPrice\":%d,\"webhookTimestamp\":%d}",
                starsPackage.getCode(),
                starsPackage.getId(),
                starsPackage.getStarsPrice(),
                request.getTimestamp());

        ArtTransactionEntity artTransaction = artRewardService.award(
                request.getUserId(),
                ART_RULE_PURCHASE_STARS,
                starsPackage.getArtAmount(), // override amount
                metadata,
                request.getTelegramChargeId(), // externalId для идемпотентности
                null // performedBy
        );

        // 7. Создание записи о покупке
        StarsPurchaseEntity purchase = new StarsPurchaseEntity();
        purchase.setUser(user);
        purchase.setUserId(request.getUserId());
        // НЕ устанавливаем invoiceIntent - invoice создавался в Python
        purchase.setInvoiceIntent(null);
        purchase.setInvoiceIntentId(null);
        purchase.setStarsPackage(starsPackage);
        purchase.setPackageId(starsPackage.getId());
        purchase.setPackageCode(starsPackage.getCode());
        purchase.setStarsPaid(request.getAmountStars());
        purchase.setArtCredited(starsPackage.getArtAmount());
        purchase.setTelegramPaymentId(request.getTelegramChargeId()); // Используем charge_id как payment_id
        purchase.setTelegramChargeId(request.getTelegramChargeId());
        purchase.setInvoicePayload(request.getInvoicePayload()); // Сохраняем для аудита
        purchase.setArtTransaction(artTransaction);
        purchase.setArtTransactionId(artTransaction.getId());

        purchase = purchaseRepository.save(purchase);

        LOGGER.info("✅ Webhook платеж успешно обработан: purchaseId={}, artCredited={}",
                purchase.getId(), purchase.getArtCredited());

        return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
    }
}

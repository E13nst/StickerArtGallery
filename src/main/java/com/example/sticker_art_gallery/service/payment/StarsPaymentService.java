package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.dto.payment.*;
import com.example.sticker_art_gallery.model.payment.*;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.*;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для обработки платежей Telegram Stars
 */
@Service
@Transactional
public class StarsPaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarsPaymentService.class);
    private static final String ART_RULE_PURCHASE_STARS = "PURCHASE_STARS";
    private static final String CURRENCY_STARS = "XTR";

    private final StarsPackageRepository starsPackageRepository;
    private final StarsInvoiceIntentRepository invoiceIntentRepository;
    private final StarsPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final TelegramBotApiService telegramBotApiService;
    private final ArtRewardService artRewardService;

    public StarsPaymentService(StarsPackageRepository starsPackageRepository,
                               StarsInvoiceIntentRepository invoiceIntentRepository,
                               StarsPurchaseRepository purchaseRepository,
                               UserRepository userRepository,
                               TelegramBotApiService telegramBotApiService,
                               ArtRewardService artRewardService) {
        this.starsPackageRepository = starsPackageRepository;
        this.invoiceIntentRepository = invoiceIntentRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.telegramBotApiService = telegramBotApiService;
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
     * Создать invoice для покупки ART за Stars
     */
    public CreateInvoiceResponse createInvoice(Long userId, String packageCode) {
        LOGGER.info("💳 Создание invoice для пользователя {} и пакета {}", userId, packageCode);

        // Проверяем существование пользователя
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        // Получаем пакет
        StarsPackageEntity starsPackage = starsPackageRepository.findByCodeAndIsEnabledTrue(packageCode)
                .orElseThrow(() -> new IllegalArgumentException("Пакет не найден или отключен: " + packageCode));

        // Генерируем уникальный payload
        String invoicePayload = UUID.randomUUID().toString();

        // Создаем намерение покупки
        StarsInvoiceIntentEntity intent = new StarsInvoiceIntentEntity();
        intent.setUser(user);
        intent.setUserId(userId);
        intent.setStarsPackage(starsPackage);
        intent.setPackageId(starsPackage.getId());
        intent.setInvoicePayload(invoicePayload);
        intent.setStatus(InvoiceStatus.PENDING);
        intent.setStarsPrice(starsPackage.getStarsPrice());
        intent.setArtAmount(starsPackage.getArtAmount());

        // Создаем invoice через Telegram Bot API
        String title = starsPackage.getName();
        String description = starsPackage.getDescription() != null 
                ? starsPackage.getDescription() 
                : starsPackage.getName();
        
        List<TelegramBotApiService.LabeledPrice> prices = List.of(
                new TelegramBotApiService.LabeledPrice(
                        starsPackage.getName(),
                        starsPackage.getStarsPrice()
                )
        );

        String invoiceUrl = telegramBotApiService.createInvoiceLink(
                title,
                description,
                invoicePayload,
                CURRENCY_STARS,
                prices
        );

        intent.setInvoiceUrl(invoiceUrl);
        intent = invoiceIntentRepository.save(intent);

        LOGGER.info("✅ Invoice создан: intentId={}, invoiceUrl={}", intent.getId(), invoiceUrl);

        StarsPackageDto packageDto = StarsPackageDto.fromEntity(starsPackage);
        return new CreateInvoiceResponse(invoiceUrl, intent.getId(), packageDto);
    }

    /**
     * Валидация платежа перед оплатой (pre_checkout_query)
     */
    @Transactional(readOnly = true)
    public ValidatePaymentResponse validatePreCheckout(String invoicePayload, Long userId, Integer totalAmount) {
        LOGGER.info("🔍 Валидация платежа: payload={}, userId={}, totalAmount={}", 
                invoicePayload, userId, totalAmount);

        Optional<StarsInvoiceIntentEntity> intentOpt = invoiceIntentRepository.findByInvoicePayload(invoicePayload);
        
        if (intentOpt.isEmpty()) {
            LOGGER.warn("❌ Invoice intent не найден: payload={}", invoicePayload);
            return ValidatePaymentResponse.invalid("Invoice не найден");
        }

        StarsInvoiceIntentEntity intent = intentOpt.get();

        // Проверяем пользователя
        if (!intent.getUserId().equals(userId)) {
            LOGGER.warn("❌ Несоответствие пользователя: expected={}, actual={}", 
                    intent.getUserId(), userId);
            return ValidatePaymentResponse.invalid("Неверный пользователь");
        }

        // Проверяем статус
        if (intent.getStatus() != InvoiceStatus.PENDING) {
            LOGGER.warn("❌ Invoice уже обработан: status={}", intent.getStatus());
            return ValidatePaymentResponse.invalid("Invoice уже обработан");
        }

        // Проверяем сумму
        if (!intent.getStarsPrice().equals(totalAmount)) {
            LOGGER.warn("❌ Несоответствие суммы: expected={}, actual={}", 
                    intent.getStarsPrice(), totalAmount);
            return ValidatePaymentResponse.invalid("Неверная сумма платежа");
        }

        LOGGER.info("✅ Платеж валиден: intentId={}", intent.getId());
        return ValidatePaymentResponse.valid();
    }

    /**
     * Обработка успешного платежа с начислением ART
     */
    public ProcessPaymentResponse processSuccessfulPayment(String telegramPaymentId, 
                                                          String telegramChargeId,
                                                          String invoicePayload,
                                                          Long userId) {
        LOGGER.info("💰 Обработка успешного платежа: paymentId={}, chargeId={}, payload={}, userId={}",
                telegramPaymentId, telegramChargeId, invoicePayload, userId);

        // Проверка идемпотентности по telegram_payment_id
        Optional<StarsPurchaseEntity> existingPurchase = purchaseRepository.findByTelegramPaymentId(telegramPaymentId);
        if (existingPurchase.isPresent()) {
            LOGGER.info("♻️ Платеж уже обработан (идемпотентность): purchaseId={}", existingPurchase.get().getId());
            StarsPurchaseEntity purchase = existingPurchase.get();
            return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
        }

        // Проверка идемпотентности по telegram_charge_id
        Optional<StarsPurchaseEntity> existingByCharge = purchaseRepository.findByTelegramChargeId(telegramChargeId);
        if (existingByCharge.isPresent()) {
            LOGGER.info("♻️ Платеж уже обработан по charge_id (идемпотентность): purchaseId={}", 
                    existingByCharge.get().getId());
            StarsPurchaseEntity purchase = existingByCharge.get();
            return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
        }

        // Находим invoice intent
        StarsInvoiceIntentEntity intent = invoiceIntentRepository.findByInvoicePayload(invoicePayload)
                .orElseThrow(() -> new IllegalArgumentException("Invoice intent не найден: " + invoicePayload));

        // Проверяем пользователя
        if (!intent.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Несоответствие пользователя");
        }

        // Проверяем статус
        if (intent.getStatus() != InvoiceStatus.PENDING) {
            LOGGER.warn("⚠️ Invoice уже обработан: status={}", intent.getStatus());
            throw new IllegalStateException("Invoice уже обработан: " + intent.getStatus());
        }

        // Начисляем ART через ArtRewardService
        String metadata = String.format("{\"packageCode\":\"%s\",\"packageId\":%d,\"starsPrice\":%d}",
                intent.getStarsPackage().getCode(),
                intent.getStarsPackage().getId(),
                intent.getStarsPrice());

        ArtTransactionEntity artTransaction = artRewardService.award(
                userId,
                ART_RULE_PURCHASE_STARS,
                intent.getArtAmount(), // override amount
                metadata,
                telegramPaymentId, // externalId для идемпотентности
                null // performedBy
        );

        // Создаем запись о покупке
        StarsPurchaseEntity purchase = new StarsPurchaseEntity();
        purchase.setUser(intent.getUser());
        purchase.setUserId(userId);
        purchase.setInvoiceIntent(intent);
        purchase.setInvoiceIntentId(intent.getId());
        purchase.setStarsPackage(intent.getStarsPackage());
        purchase.setPackageId(intent.getStarsPackage().getId());
        purchase.setPackageCode(intent.getStarsPackage().getCode());
        purchase.setStarsPaid(intent.getStarsPrice());
        purchase.setArtCredited(intent.getArtAmount());
        purchase.setTelegramPaymentId(telegramPaymentId);
        purchase.setTelegramChargeId(telegramChargeId);
        purchase.setInvoicePayload(invoicePayload);
        purchase.setArtTransaction(artTransaction);
        purchase.setArtTransactionId(artTransaction.getId());

        purchase = purchaseRepository.save(purchase);

        // Обновляем статус intent
        intent.setStatus(InvoiceStatus.COMPLETED);
        invoiceIntentRepository.save(intent);

        LOGGER.info("✅ Платеж успешно обработан: purchaseId={}, artCredited={}", 
                purchase.getId(), purchase.getArtCredited());

        return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
    }

    /**
     * Получить историю покупок пользователя
     */
    @Transactional(readOnly = true)
    public Page<StarsPurchaseDto> getPurchaseHistory(Long userId, Pageable pageable) {
        Page<StarsPurchaseEntity> purchases = purchaseRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return purchases.map(StarsPurchaseDto::fromEntity);
    }
}

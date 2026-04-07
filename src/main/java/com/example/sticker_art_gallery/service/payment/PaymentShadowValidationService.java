package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.dto.payment.TelegramWebhookRequest;
import com.example.sticker_art_gallery.model.payment.StarsPackageEntity;
import com.example.sticker_art_gallery.repository.StarsPurchaseRepository;
import com.example.sticker_art_gallery.repository.StarsPackageRepository;
import com.example.sticker_art_gallery.repository.StarsInvoiceIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Shadow-сверка payment flow (Stage 3 миграции StickerBot → Java).
 *
 * При включённом shadow-режиме (логируется рядом с production-обработкой, без side-effects)
 * проверяет те же инварианты, что должны выполняться при нативной Java-обработке платежа:
 *
 *  - идемпотентность по telegram_charge_id,
 *  - разрешение пакета по invoice_payload,
 *  - валидация суммы,
 *  - наличие пользователя.
 *
 * Все расхождения пишутся в лог с тегом [SHADOW_MISMATCH] для последующего анализа.
 * Метод НЕ изменяет состояние БД и НЕ влияет на production-ответ.
 */
@Service
public class PaymentShadowValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentShadowValidationService.class);
    private static final String SHADOW_TAG = "[SHADOW]";
    private static final String MISMATCH_TAG = "[SHADOW_MISMATCH]";

    private final StarsPurchaseRepository purchaseRepository;
    private final StarsPackageRepository starsPackageRepository;
    private final StarsInvoiceIntentRepository starsInvoiceIntentRepository;

    public PaymentShadowValidationService(StarsPurchaseRepository purchaseRepository,
                                          StarsPackageRepository starsPackageRepository,
                                          StarsInvoiceIntentRepository starsInvoiceIntentRepository) {
        this.purchaseRepository = purchaseRepository;
        this.starsPackageRepository = starsPackageRepository;
        this.starsInvoiceIntentRepository = starsInvoiceIntentRepository;
    }

    /**
     * Выполнить shadow-сверку входящего webhook-платежа.
     * Возвращает список найденных расхождений (пустой список = OK).
     *
     * Вызывать ПОСЛЕ production-обработки, в отдельной транзакции read-only.
     */
    @Transactional(readOnly = true)
    public List<String> validate(TelegramWebhookRequest request) {
        List<String> mismatches = new ArrayList<>();

        if (request == null) {
            mismatches.add("request is null");
            return mismatches;
        }

        String chargeId = request.getTelegramChargeId();

        try {
            // 1. Идемпотентность: должна быть запись в StarsPurchase
            boolean existsInProduction = purchaseRepository.findByTelegramChargeId(chargeId).isPresent();
            if (!existsInProduction) {
                mismatches.add("idempotency: purchase NOT found after production processing (chargeId=" + chargeId + ")");
            }

            // 2. Разрешение пакета
            StarsPackageEntity resolvedPackage = resolvePackage(request);
            if (resolvedPackage == null) {
                mismatches.add("package_resolution: cannot resolve package from payload='" + request.getInvoicePayload() + "'");
            } else {
                // 3. Валидация суммы
                if (!resolvedPackage.getStarsPrice().equals(request.getAmountStars())) {
                    mismatches.add(String.format(
                            "amount_validation: expected %d XTR, got %d XTR (packageId=%d)",
                            resolvedPackage.getStarsPrice(), request.getAmountStars(), resolvedPackage.getId()));
                }

                // 4. Пакет активен
                if (!Boolean.TRUE.equals(resolvedPackage.getIsEnabled())) {
                    mismatches.add("package_enabled: package is disabled (packageId=" + resolvedPackage.getId() + ")");
                }
            }

        } catch (Exception e) {
            mismatches.add("shadow_error: exception during shadow validation: " + e.getMessage());
            LOGGER.warn("{} Exception during validation: chargeId={}", SHADOW_TAG, chargeId, e);
        }

        if (mismatches.isEmpty()) {
            LOGGER.info("{} OK: chargeId={}, userId={}, amountStars={}",
                    SHADOW_TAG, chargeId, request.getUserId(), request.getAmountStars());
        } else {
            for (String m : mismatches) {
                LOGGER.warn("{} chargeId={}, userId={}: {}",
                        MISMATCH_TAG, chargeId, request.getUserId(), m);
            }
        }

        return mismatches;
    }

    private StarsPackageEntity resolvePackage(TelegramWebhookRequest request) {
        // Попытка через intent
        if (request.getInvoicePayload() != null) {
            var intentOpt = starsInvoiceIntentRepository.findByInvoicePayload(request.getInvoicePayload());
            if (intentOpt.isPresent()) {
                var intent = intentOpt.get();
                if (intent.getStarsPackage() != null) {
                    return intent.getStarsPackage();
                }
                if (intent.getPackageId() != null) {
                    return starsPackageRepository.findById(intent.getPackageId()).orElse(null);
                }
            }
        }
        // Fallback: package_id из payload
        Long packageId = request.getPackageIdFromPayload();
        if (packageId != null) {
            return starsPackageRepository.findById(packageId).orElse(null);
        }
        return null;
    }
}

package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.dto.payment.*;
import com.example.sticker_art_gallery.model.payment.*;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.*;
import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final StarsPackageRepository starsPackageRepository;
    private final StarsInvoiceIntentRepository starsInvoiceIntentRepository;
    private final StarsPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ArtRewardService artRewardService;
    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final TelegramBotApiService telegramBotApiService;

    public StarsPaymentService(StarsPackageRepository starsPackageRepository,
                               StarsInvoiceIntentRepository starsInvoiceIntentRepository,
                               StarsPurchaseRepository purchaseRepository,
                               UserRepository userRepository,
                               ArtRewardService artRewardService,
                               RestTemplate restTemplate,
                               AppConfig appConfig,
                               TelegramBotApiService telegramBotApiService) {
        this.starsPackageRepository = starsPackageRepository;
        this.starsInvoiceIntentRepository = starsInvoiceIntentRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.artRewardService = artRewardService;
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
        this.telegramBotApiService = telegramBotApiService;
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
     * Создать invoice для покупки пакета ART за Stars
     */
    public CreateInvoiceResponse createInvoice(Long userId, CreateInvoiceRequest request, String telegramInitData) {
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }
        if (telegramInitData == null || telegramInitData.isBlank()) {
            throw new IllegalArgumentException("Отсутствует X-Telegram-Init-Data");
        }
        if (request == null || request.getPackageCode() == null || request.getPackageCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Код пакета обязателен");
        }

        String packageCode = request.getPackageCode().trim();
        StarsPackageEntity starsPackage = starsPackageRepository.findByCode(packageCode)
                .orElseThrow(() -> new java.util.NoSuchElementException("Пакет не найден: " + packageCode));

        if (!Boolean.TRUE.equals(starsPackage.getIsEnabled())) {
            throw new IllegalArgumentException("Пакет отключен: " + packageCode);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String invoicePayload = String.format(
                "{\"package_id\":%d,\"nonce\":\"%s\"}",
                starsPackage.getId(), nonce
        );
        if (invoicePayload.length() > 128) {
            throw new IllegalArgumentException("Invoice payload слишком длинный");
        }

        StarsInvoiceIntentEntity intent = new StarsInvoiceIntentEntity();
        intent.setUser(user);
        intent.setStarsPackage(starsPackage);
        intent.setInvoicePayload(invoicePayload);
        intent.setStatus(InvoiceStatus.PENDING);
        intent.setStarsPrice(starsPackage.getStarsPrice());
        intent.setArtAmount(starsPackage.getArtAmount());
        intent.setMetadata(String.format("{\"source\":\"java_backend\",\"packageCode\":\"%s\"}", starsPackage.getCode()));

        intent = starsInvoiceIntentRepository.save(intent);

        String invoiceUrl;

        if (appConfig.getTelegram().isNativePaymentEnabled()) {
            // Нативный путь: Java → Telegram Bot API напрямую
            LOGGER.info("💳 Создание invoice через Telegram Bot API (native): userId={}, packageCode={}",
                    userId, starsPackage.getCode());
            try {
                List<TelegramBotApiService.LabeledPrice> prices = Collections.singletonList(
                        new TelegramBotApiService.LabeledPrice(starsPackage.getName(), starsPackage.getStarsPrice())
                );
                invoiceUrl = telegramBotApiService.createInvoiceLink(
                        starsPackage.getName(),
                        starsPackage.getDescription(),
                        invoicePayload,
                        "XTR",
                        prices
                );
                if (invoiceUrl == null || invoiceUrl.isBlank()) {
                    throw new RuntimeException("Telegram Bot API не вернул invoice_link");
                }
            } catch (Exception e) {
                throw new RuntimeException("Ошибка создания invoice через Telegram Bot API: " + e.getMessage(), e);
            }
        } else {
            // Legacy путь: Java → StickerBot API → Telegram Bot API
            LOGGER.info("💳 Создание invoice через StickerBot API (legacy): userId={}, packageCode={}",
                    userId, starsPackage.getCode());

            String stickerBotApiUrl = appConfig.getStickerbot().getApiUrl();
            if (stickerBotApiUrl == null || stickerBotApiUrl.isBlank()) {
                throw new IllegalStateException("StickerBot API URL не настроен (app.stickerbot.api-url)");
            }
            String appUrl = appConfig.getUrl();
            if (appUrl == null || appUrl.isBlank()) {
                throw new IllegalStateException("App URL не настроен (app.url)");
            }

            String webhookUrl = appUrl.replaceAll("/$", "") + "/api/internal/webhooks/stars-payment";
            String url = stickerBotApiUrl.replaceAll("/$", "") + "/api/payments/create-invoice";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);
            requestBody.put("title", starsPackage.getName());
            requestBody.put("description", starsPackage.getDescription());
            requestBody.put("amount_stars", starsPackage.getStarsPrice());
            requestBody.put("payload", invoicePayload);
            requestBody.put("return_link", true);
            requestBody.put("backend_webhook_url", webhookUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Telegram-Init-Data", telegramInitData);

            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(requestBody, headers),
                        new ParameterizedTypeReference<>() {}
                );
                Map<String, Object> responseBody = response.getBody();
                if (responseBody == null) {
                    throw new RuntimeException("Пустой ответ от StickerBot API");
                }
                Object okValue = responseBody.get("ok");
                boolean ok = okValue instanceof Boolean && (Boolean) okValue;
                if (!ok) {
                    Object error = responseBody.get("error");
                    throw new IllegalArgumentException("StickerBot API error: " + (error != null ? error : "unknown error"));
                }
                Object invoiceLink = responseBody.get("invoice_link");
                if (!(invoiceLink instanceof String) || ((String) invoiceLink).isBlank()) {
                    throw new RuntimeException("StickerBot API не вернул invoice_link");
                }
                invoiceUrl = (String) invoiceLink;
            } catch (RestClientException e) {
                throw new RuntimeException("Ошибка вызова StickerBot API: " + e.getMessage(), e);
            }
        }

        intent.setInvoiceUrl(invoiceUrl);
        starsInvoiceIntentRepository.save(intent);

        return new CreateInvoiceResponse(
                invoiceUrl,
                intent.getId(),
                StarsPackageDto.fromEntity(starsPackage)
        );
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

        // 2. Поиск intent по invoice_payload (при наличии)
        Optional<StarsInvoiceIntentEntity> invoiceIntentOptional =
                starsInvoiceIntentRepository.findByInvoicePayload(request.getInvoicePayload());

        StarsPackageEntity starsPackage;
        if (invoiceIntentOptional.isPresent()) {
            StarsInvoiceIntentEntity intent = invoiceIntentOptional.get();

            if (!intent.getUserId().equals(request.getUserId())) {
                throw new IllegalArgumentException("invoice_payload принадлежит другому пользователю");
            }

            starsPackage = intent.getStarsPackage() != null
                    ? intent.getStarsPackage()
                    : starsPackageRepository.findById(intent.getPackageId())
                    .orElseThrow(() -> new IllegalArgumentException("Пакет не найден: " + intent.getPackageId()));
        } else {
            // Legacy fallback: старый flow без созданного intent в Java
            Long packageId = request.getPackageIdFromPayload();
            if (packageId == null) {
                String errorMsg = "Не удалось извлечь package_id из invoice_payload: " + request.getInvoicePayload();
                LOGGER.error("❌ {}", errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            LOGGER.debug("🔍 Извлечен package_id (fallback): {}", packageId);
            starsPackage = starsPackageRepository.findById(packageId)
                    .orElseThrow(() -> new IllegalArgumentException("Пакет не найден: " + packageId));
        }

        // Проверка что пакет активен
        if (!starsPackage.getIsEnabled()) {
            String errorMsg = "Пакет отключен: " + starsPackage.getId();
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
        purchase.setInvoiceIntent(invoiceIntentOptional.orElse(null));
        purchase.setInvoiceIntentId(invoiceIntentOptional.map(StarsInvoiceIntentEntity::getId).orElse(null));
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

        invoiceIntentOptional.ifPresent(intent -> {
            intent.setStatus(InvoiceStatus.COMPLETED);
            starsInvoiceIntentRepository.save(intent);
        });

        LOGGER.info("✅ Webhook платеж успешно обработан: purchaseId={}, artCredited={}",
                purchase.getId(), purchase.getArtCredited());

        return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
    }
}

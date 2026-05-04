package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.payment.*;
import com.example.sticker_art_gallery.model.payment.*;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.*;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TonArtPaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TonArtPaymentService.class);
    private static final String ASSET_TON = "TON";
    private static final EnumSet<TonPaymentStatus> ACTIVE_INTENT_STATUSES = EnumSet.of(
            TonPaymentStatus.CREATED,
            TonPaymentStatus.READY,
            TonPaymentStatus.SENT
    );

    private final StarsPackageRepository starsPackageRepository;
    private final TonPaymentIntentRepository intentRepository;
    private final TonPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ArtRewardService artRewardService;
    private final TonPayAdapterClient tonPayAdapterClient;
    private final TonPaymentSettingsService tonPaymentSettingsService;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    public TonArtPaymentService(StarsPackageRepository starsPackageRepository,
                                TonPaymentIntentRepository intentRepository,
                                TonPurchaseRepository purchaseRepository,
                                UserRepository userRepository,
                                ArtRewardService artRewardService,
                                TonPayAdapterClient tonPayAdapterClient,
                                TonPaymentSettingsService tonPaymentSettingsService,
                                AppConfig appConfig,
                                ObjectMapper objectMapper) {
        this.starsPackageRepository = starsPackageRepository;
        this.intentRepository = intentRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.artRewardService = artRewardService;
        this.tonPayAdapterClient = tonPayAdapterClient;
        this.tonPaymentSettingsService = tonPaymentSettingsService;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    public CreateTonPaymentResponse createPayment(Long userId, CreateTonPaymentRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }
        if (request == null || request.getPackageCode() == null || request.getPackageCode().isBlank()) {
            throw new IllegalArgumentException("Код пакета обязателен");
        }
        if (request.getSenderAddress() == null || request.getSenderAddress().isBlank()) {
            throw new IllegalArgumentException("Адрес отправителя обязателен");
        }

        String senderAddress = request.getSenderAddress().trim();

        String packageCode = request.getPackageCode().trim();
        Optional<TonPaymentIntentEntity> activeIntentOptional = intentRepository
                .findFirstByUserIdAndPackageCodeAndStatusInOrderByCreatedAtDesc(
                        userId, packageCode, ACTIVE_INTENT_STATUSES
                );
        if (activeIntentOptional.isPresent()) {
            TonPaymentIntentEntity activeIntent = activeIntentOptional.get();
            if (!activeIntent.getSenderAddress().equals(senderAddress)) {
                throw new TonPaymentCreateConflictException(
                        TonPaymentCreateConflictResponse.senderAddressMismatch(
                                activeIntent.getId(),
                                activeIntent.getStatus(),
                                activeIntent.getPackageCode(),
                                activeIntent.getSenderAddress(),
                                senderAddress
                        )
                );
            }
            LOGGER.info("Возвращаем активный TON intent без создания нового: intentId={}, userId={}, package={}",
                    activeIntent.getId(), userId, packageCode);
            return toCreateResponse(activeIntent);
        }

        String merchantWallet = tonPaymentSettingsService.resolveMerchantWalletAddress();

        StarsPackageEntity starsPackage = starsPackageRepository.findByCode(packageCode)
                .orElseThrow(() -> new java.util.NoSuchElementException("Пакет не найден: " + packageCode));
        if (!Boolean.TRUE.equals(starsPackage.getIsEnabled())) {
            throw new IllegalArgumentException("Пакет отключен: " + packageCode);
        }
        if (starsPackage.getTonPriceNano() == null || starsPackage.getTonPriceNano() <= 0) {
            throw new IllegalArgumentException("TON-оплата для пакета не настроена: " + packageCode);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        TonPaymentIntentEntity intent = new TonPaymentIntentEntity();
        intent.setUser(user);
        intent.setStarsPackage(starsPackage);
        intent.setPackageCode(starsPackage.getCode());
        intent.setStatus(TonPaymentStatus.CREATED);
        intent.setExpectedAmountNano(starsPackage.getTonPriceNano());
        intent.setAsset(ASSET_TON);
        intent.setArtAmount(starsPackage.getArtAmount());
        intent.setSenderAddress(senderAddress);
        intent.setRecipientAddress(merchantWallet.trim());
        intent.setMetadata(toJsonSilently(new PaymentMetadata("java_backend", appConfig.getTonpay().getChain())));
        intent = intentRepository.save(intent);

        try {
            TonPayCreateTransferResponse transfer = tonPayAdapterClient.createTransfer(new TonPayCreateTransferRequest(
                    intent.getId(),
                    intent.getExpectedAmountNano(),
                    intent.getAsset(),
                    intent.getRecipientAddress(),
                    intent.getSenderAddress(),
                    "ART " + intent.getPackageCode(),
                    "ART order " + intent.getId()
            ));

            intent.setReference(transfer.getReference());
            intent.setBodyBase64Hash(transfer.getBodyBase64Hash());
            TonConnectTransactionDto transaction = createTonConnectTransaction(transfer.getMessage());
            intent.setTonConnectMessage(toJson(transaction));
            intent.setStatus(TonPaymentStatus.READY);
            intent = intentRepository.save(intent);

            LOGGER.info("Создан TON Pay intent: intentId={}, userId={}, package={}, reference={}",
                    intent.getId(), userId, packageCode, intent.getReference());

            return toCreateResponse(intent, transaction);
        } catch (RuntimeException e) {
            intent.setStatus(TonPaymentStatus.FAILED);
            intent.setFailureReason(trimReason(e.getMessage()));
            intentRepository.save(intent);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public TonPaymentStatusResponse getStatus(Long userId, Long intentId) {
        TonPaymentIntentEntity intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("TON intent не найден: " + intentId));
        if (!intent.getUserId().equals(userId)) {
            throw new IllegalArgumentException("TON intent принадлежит другому пользователю");
        }
        String txHash = intent.getReference() == null
                ? null
                : purchaseRepository.findByReference(intent.getReference()).map(TonPurchaseEntity::getTxHash).orElse(null);
        return TonPaymentStatusResponse.fromEntity(intent, txHash);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public ProcessPaymentResponse processWebhook(TonPayWebhookRequest request) {
        TonPayWebhookRequest.Data data = request != null ? request.getData() : null;
        if (data == null || data.getReference() == null || data.getReference().isBlank()) {
            throw new IllegalArgumentException("TON Pay webhook без reference");
        }

        TonPaymentIntentEntity intent = intentRepository.findWithLockByReference(data.getReference())
                .orElseThrow(() -> new IllegalArgumentException("TON intent не найден по reference: " + data.getReference()));

        Optional<TonPurchaseEntity> existingPurchase = purchaseRepository.findByReference(data.getReference());
        if (existingPurchase.isPresent()) {
            TonPurchaseEntity purchase = existingPurchase.get();
            LOGGER.info("Повторный TON webhook пропущен: reference={}, purchaseId={}", data.getReference(), purchase.getId());
            return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
        }

        if (!"transfer.completed".equals(request.getEvent())) {
            markFailed(intent, "Unexpected TON Pay event: " + request.getEvent());
            return ProcessPaymentResponse.failure(intent.getFailureReason());
        }

        if (!"success".equalsIgnoreCase(data.getStatus())) {
            markFailed(intent, "TON transfer failed: " + safe(data.getErrorMessage()));
            return ProcessPaymentResponse.failure(intent.getFailureReason());
        }

        validateWebhookData(intent, data);

        if (purchaseRepository.existsByTxHash(data.getTxHash())) {
            markFailed(intent, "TON txHash уже обработан: " + data.getTxHash());
            return ProcessPaymentResponse.failure(intent.getFailureReason());
        }

        String metadata = toJsonSilently(new PurchaseMetadata(
                intent.getPackageCode(),
                intent.getPackageId(),
                intent.getExpectedAmountNano(),
                data.getReference(),
                data.getBodyBase64Hash(),
                data.getTxHash(),
                data.getTraceId()
        ));
        ArtTransactionEntity artTransaction = artRewardService.award(
                intent.getUserId(),
                ArtRewardService.RULE_PURCHASE_TON,
                intent.getArtAmount(),
                metadata,
                "tonpay:" + data.getReference(),
                null
        );

        TonPurchaseEntity purchase = new TonPurchaseEntity();
        purchase.setUser(intent.getUser());
        purchase.setIntent(intent);
        purchase.setStarsPackage(intent.getStarsPackage());
        purchase.setPackageCode(intent.getPackageCode());
        purchase.setTonPaidNano(parseRawAmount(data));
        purchase.setAsset(intent.getAsset());
        purchase.setArtCredited(intent.getArtAmount());
        purchase.setReference(data.getReference());
        purchase.setBodyBase64Hash(data.getBodyBase64Hash());
        purchase.setTxHash(data.getTxHash());
        purchase.setSenderAddress(data.getSenderAddr());
        purchase.setRecipientAddress(data.getRecipientAddr());
        purchase.setArtTransaction(artTransaction);
        purchase.setMetadata(metadata);
        purchase = purchaseRepository.save(purchase);

        intent.setStatus(TonPaymentStatus.COMPLETED);
        intent.setFailureReason(null);
        intentRepository.save(intent);

        LOGGER.info("TON Pay покупка завершена: intentId={}, purchaseId={}, art={}",
                intent.getId(), purchase.getId(), purchase.getArtCredited());
        return ProcessPaymentResponse.success(purchase.getId(), purchase.getArtCredited());
    }

    private void validateWebhookData(TonPaymentIntentEntity intent, TonPayWebhookRequest.Data data) {
        long rawAmount = parseRawAmount(data);
        if (rawAmount != intent.getExpectedAmountNano()) {
            throw validationFailure(intent, "TON amount mismatch: expected=" + intent.getExpectedAmountNano() + ", got=" + rawAmount);
        }
        if (!intent.getAsset().equals(data.getAsset())) {
            throw validationFailure(intent, "TON asset mismatch: expected=" + intent.getAsset() + ", got=" + data.getAsset());
        }
        if (!intent.getRecipientAddress().equals(data.getRecipientAddr())) {
            throw validationFailure(intent, "TON recipient mismatch");
        }
        if (!intent.getSenderAddress().equals(data.getSenderAddr())) {
            throw validationFailure(intent, "TON sender mismatch");
        }
        if (intent.getBodyBase64Hash() != null && data.getBodyBase64Hash() != null
                && !intent.getBodyBase64Hash().equals(data.getBodyBase64Hash())) {
            throw validationFailure(intent, "TON bodyBase64Hash mismatch");
        }
        if (data.getTxHash() == null || data.getTxHash().isBlank()) {
            throw validationFailure(intent, "TON txHash missing");
        }
    }

    private IllegalArgumentException validationFailure(TonPaymentIntentEntity intent, String reason) {
        markFailed(intent, reason);
        return new IllegalArgumentException(reason);
    }

    private void markFailed(TonPaymentIntentEntity intent, String reason) {
        intent.setStatus(TonPaymentStatus.FAILED);
        intent.setFailureReason(trimReason(reason));
        intentRepository.save(intent);
        LOGGER.warn("TON Pay intent failed: intentId={}, reason={}", intent.getId(), reason);
    }

    private long parseRawAmount(TonPayWebhookRequest.Data data) {
        if (data.getRawAmount() != null && !data.getRawAmount().isBlank()) {
            return Long.parseLong(data.getRawAmount());
        }
        if (data.getAmount() == null || data.getAmount().isBlank()) {
            throw new IllegalArgumentException("TON Pay webhook без amount/rawAmount");
        }
        return new BigDecimal(data.getAmount())
                .multiply(BigDecimal.valueOf(1_000_000_000L))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать JSON", e);
        }
    }

    private String toJsonSilently(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private CreateTonPaymentResponse toCreateResponse(TonPaymentIntentEntity intent) {
        return toCreateResponse(intent, parseTonConnectTransaction(intent.getTonConnectMessage()));
    }

    private CreateTonPaymentResponse toCreateResponse(TonPaymentIntentEntity intent, TonConnectTransactionDto transaction) {
        return new CreateTonPaymentResponse(
                intent.getId(),
                intent.getStatus(),
                intent.getReference(),
                intent.getBodyBase64Hash(),
                intent.getExpectedAmountNano(),
                intent.getAsset(),
                intent.getRecipientAddress(),
                StarsPackageDto.fromEntity(intent.getStarsPackage()),
                transaction
        );
    }

    private TonConnectTransactionDto parseTonConnectTransaction(String rawTransactionJson) {
        if (rawTransactionJson == null || rawTransactionJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawTransactionJson, TonConnectTransactionDto.class);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Не удалось распарсить сохранённый tonConnectMessage: {}", e.getMessage());
            return null;
        }
    }

    private TonConnectTransactionDto createTonConnectTransaction(TonConnectMessageDto message) {
        long validUntil = Instant.now().getEpochSecond() + 300;
        return new TonConnectTransactionDto(validUntil, List.of(message));
    }

    private record PaymentMetadata(String source, String chain) {}

    private record PurchaseMetadata(String packageCode,
                                    Long packageId,
                                    Long tonPaidNano,
                                    String reference,
                                    String bodyBase64Hash,
                                    String txHash,
                                    String traceId) {}
}

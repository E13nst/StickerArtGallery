package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.transaction.*;
import com.example.sticker_art_gallery.model.transaction.*;
import com.example.sticker_art_gallery.service.transaction.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для работы с TON-транзакциями
 */
@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "TON транзакции", description = "Управление TON-транзакциями в Telegram Mini App")
@SecurityRequirement(name = "TelegramInitData")
public class TransactionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionIntentService intentService;
    private final TonBlockchainService blockchainService;
    private final BlockchainTransactionRepository blockchainTransactionRepository;

    @Autowired
    public TransactionController(
            TransactionIntentService intentService,
            TonBlockchainService blockchainService,
            BlockchainTransactionRepository blockchainTransactionRepository) {
        this.intentService = intentService;
        this.blockchainService = blockchainService;
        this.blockchainTransactionRepository = blockchainTransactionRepository;
    }

    /**
     * Подготовка транзакции
     * Создает TransactionIntent и TransactionLegs в одной транзакции (Правила 1 и 2)
     */
    @PostMapping("/prepare")
    @Operation(
        summary = "Подготовка транзакции",
        description = "Создает намерение транзакции (TransactionIntent) и части транзакции (TransactionLegs). " +
                     "Intent создается первым, затем Legs в одной транзакции БД."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Транзакция подготовлена успешно",
            content = @Content(schema = @Schema(implementation = PrepareTransactionResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "intentId": 1,
                        "intentType": "DONATION",
                        "status": "CREATED",
                        "amountNano": 1000000000,
                        "currency": "TON",
                        "legs": [
                            {
                                "id": 1,
                                "legType": "MAIN",
                                "toEntityId": 123,
                                "toWalletAddress": "EQDummyWalletAddressForEntity_123",
                                "amountNano": 1000000000
                            }
                        ]
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PrepareTransactionResponse> prepareTransaction(
            @Valid @RequestBody PrepareTransactionRequest request) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Не удалось определить ID текущего пользователя");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            LOGGER.info("Подготовка транзакции: userId={}, subjectEntityId={}, amountNano={}, intentType={}",
                       currentUserId, request.getSubjectEntityId(), request.getAmountNano(), request.getIntentType());

            // Правила 1 и 2: Создаем Intent первым, затем Legs в одной транзакции
            TransactionIntentEntity intent = intentService.createIntent(
                    request.getIntentType() != null ? request.getIntentType() : IntentType.DONATION,
                    currentUserId,
                    request.getSubjectEntityId(),
                    request.getAmountNano(),
                    null // metadata можно добавить позже
            );

            // Получаем legs для ответа
            List<TransactionLegEntity> legs = intentService.getLegsForIntent(intent.getId());
            List<TransactionLegDto> legDtos = legs.stream()
                    .map(leg -> {
                        TransactionLegDto dto = new TransactionLegDto();
                        dto.setId(leg.getId());
                        dto.setLegType(leg.getLegType());
                        dto.setToEntityId(leg.getToEntity() != null ? leg.getToEntity().getId() : null);
                        dto.setToWalletAddress(leg.getToWalletAddress());
                        dto.setAmountNano(leg.getAmountNano());
                        return dto;
                    })
                    .collect(Collectors.toList());

            PrepareTransactionResponse response = new PrepareTransactionResponse(
                    intent.getId(),
                    intent.getIntentType(),
                    intent.getStatus(),
                    intent.getAmountNano(),
                    intent.getCurrency(),
                    legDtos
            );

            LOGGER.info("✅ Транзакция подготовлена: intentId={}, legsCount={}", intent.getId(), legDtos.size());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при подготовке транзакции: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при подготовке транзакции: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Подтверждение транзакции
     * Проверяет транзакцию в блокчейне и обновляет статус Intent
     */
    @PostMapping("/confirm")
    @Operation(
        summary = "Подтверждение транзакции",
        description = "Проверяет транзакцию в блокчейне TON и обновляет статус намерения на CONFIRMED или FAILED"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Транзакция подтверждена",
            content = @Content(schema = @Schema(implementation = ConfirmTransactionResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "intentId": 1,
                        "status": "CONFIRMED",
                        "txHash": "0x1234567890abcdef",
                        "success": true,
                        "message": "Транзакция успешно подтверждена"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
        @ApiResponse(responseCode = "404", description = "Намерение не найдено"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<ConfirmTransactionResponse> confirmTransaction(
            @Valid @RequestBody ConfirmTransactionRequest request) {
        try {
            LOGGER.info("Подтверждение транзакции: intentId={}, txHash={}, fromWallet={}",
                       request.getIntentId(), request.getTxHash(), request.getFromWallet());

            // Найти Intent
            TransactionIntentEntity intent = intentService.findById(request.getIntentId())
                    .orElseThrow(() -> new IllegalArgumentException("Намерение с ID " + request.getIntentId() + " не найдено"));

            // Проверить статус
            if (intent.getStatus() != IntentStatus.CREATED && intent.getStatus() != IntentStatus.SENT) {
                LOGGER.warn("⚠️ Неверный статус намерения: intentId={}, status={}", intent.getId(), intent.getStatus());
                return ResponseEntity.badRequest().build();
            }

            // Получить legs для определения получателя
            List<TransactionLegEntity> legs = intentService.getLegsForIntent(intent.getId());
            if (legs.isEmpty()) {
                LOGGER.error("❌ У намерения нет legs: intentId={}", intent.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Для упрощения берем первый MAIN leg
            TransactionLegEntity mainLeg = legs.stream()
                    .filter(leg -> leg.getLegType() == LegType.MAIN)
                    .findFirst()
                    .orElse(legs.get(0));

            // Проверить, что транзакция с таким tx_hash еще не существует (защита от дубликатов)
            if (blockchainTransactionRepository.existsByTxHash(request.getTxHash())) {
                LOGGER.warn("⚠️ Транзакция с txHash={} уже существует", request.getTxHash());
                return ResponseEntity.badRequest().build();
            }

            // Проверить транзакцию в блокчейне (stub)
            boolean isValid = blockchainService.verifyTransaction(
                    request.getTxHash(),
                    request.getFromWallet(),
                    mainLeg.getToWalletAddress(),
                    mainLeg.getAmountNano()
            );

            // Создать BlockchainTransaction
            BlockchainTransactionEntity blockchainTx = new BlockchainTransactionEntity();
            blockchainTx.setIntent(intent);
            blockchainTx.setTxHash(request.getTxHash());
            blockchainTx.setFromWallet(request.getFromWallet());
            blockchainTx.setToWallet(mainLeg.getToWalletAddress());
            blockchainTx.setAmountNano(mainLeg.getAmountNano());
            blockchainTx.setCurrency(intent.getCurrency());
            blockchainTransactionRepository.save(blockchainTx);

            // Обновить статус Intent
            IntentStatus newStatus = isValid ? IntentStatus.CONFIRMED : IntentStatus.FAILED;
            intentService.updateStatus(intent.getId(), newStatus);

            ConfirmTransactionResponse response = new ConfirmTransactionResponse(
                    intent.getId(),
                    newStatus,
                    request.getTxHash(),
                    isValid,
                    isValid ? "Транзакция успешно подтверждена" : "Транзакция не прошла проверку"
            );

            LOGGER.info("✅ Транзакция подтверждена: intentId={}, status={}, isValid={}", 
                      intent.getId(), newStatus, isValid);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при подтверждении транзакции: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при подтверждении транзакции: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Извлечь ID текущего пользователя из SecurityContext
     */
    private Long getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication authentication = 
                    SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при извлечении userId: {}", e.getMessage());
            return null;
        }
    }
}


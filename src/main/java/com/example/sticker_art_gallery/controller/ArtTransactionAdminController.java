package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.ArtTransactionDto;
import com.example.sticker_art_gallery.dto.CreateArtTransactionRequest;
import com.example.sticker_art_gallery.dto.CreateArtTransactionResponse;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.service.messaging.StickerBotMessageService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Админ контроллер для журнала ART транзакций и создания транзакций вручную.
 */
@RestController
@RequestMapping("/api/admin/art-transactions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "ART Транзакции (Admin)", description = "Журнал ART транзакций всех пользователей (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class ArtTransactionAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtTransactionAdminController.class);

    public static final String RULE_ADMIN_MANUAL_CREDIT = "ADMIN_MANUAL_CREDIT";
    public static final String RULE_ADMIN_MANUAL_DEBIT = "ADMIN_MANUAL_DEBIT";

    private final ArtRewardService artRewardService;
    private final StickerBotMessageService stickerBotMessageService;

    @Autowired
    public ArtTransactionAdminController(ArtRewardService artRewardService,
                                         StickerBotMessageService stickerBotMessageService) {
        this.artRewardService = artRewardService;
        this.stickerBotMessageService = stickerBotMessageService;
    }

    /**
     * Получить все ART транзакции с пагинацией
     */
    @GetMapping
    @Operation(
            summary = "Получить все ART транзакции",
            description = "Возвращает журнал всех ART транзакций с пагинацией (только для ADMIN)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список транзакций получен",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<ArtTransactionDto>> getAllTransactions(
            @ParameterObject @Valid PageRequest pageRequest) {
        try {
            Page<ArtTransactionEntity> transactions = artRewardService.findAllTransactions(pageRequest.toPageable());
            PageResponse<ArtTransactionDto> response = PageResponse.of(
                    transactions.map(ArtTransactionDto::fromEntity)
            );
            LOGGER.info("💰 Возвращено {} ART транзакций", response.getContent().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка ART транзакций: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Создать ART-транзакцию (начисление или списание) и опционально отправить сообщение пользователю ботом.
     * При ошибке отправки сообщения транзакция сохраняется, в ответе возвращаются messageSent=false и messageError.
     */
    @PostMapping
    @Operation(
            summary = "Создать ART-транзакцию (ADMIN)",
            description = "Создаёт начисление (amount > 0) или списание (amount < 0) ART и при необходимости отправляет сообщение пользователю в боте."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Транзакция создана",
                    content = @Content(schema = @Schema(implementation = CreateArtTransactionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Некорректные данные (например amount = 0)"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<CreateArtTransactionResponse> createTransaction(
            @RequestBody @Valid CreateArtTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount() == 0) {
            return ResponseEntity.badRequest().build();
        }
        Long adminId = getCurrentUserId();
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String ruleCode = request.getAmount() > 0 ? RULE_ADMIN_MANUAL_CREDIT : RULE_ADMIN_MANUAL_DEBIT;
        long overrideAmount = Math.abs(request.getAmount());
        String metadata = buildMetadataJson(request.getMessage());
        String externalId = "admin-manual-" + UUID.randomUUID();

        ArtTransactionEntity transaction = artRewardService.award(
                request.getUserId(),
                ruleCode,
                overrideAmount,
                metadata,
                externalId,
                adminId
        );

        boolean messageSent = false;
        String messageError = null;
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            try {
                stickerBotMessageService.sendPlainTextToUser(request.getUserId(), request.getMessage());
                messageSent = true;
            } catch (Exception e) {
                messageError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                LOGGER.warn("⚠️ ART-транзакция создана, но не удалось отправить сообщение пользователю {}: {}",
                        request.getUserId(), messageError);
            }
        }

        CreateArtTransactionResponse response = CreateArtTransactionResponse.success(
                ArtTransactionDto.fromEntity(transaction),
                messageSent,
                messageError
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private static String buildMetadataJson(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String escaped = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "{\"message\":\"" + escaped + "\"}";
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }
}

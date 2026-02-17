package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.ArtTransactionDto;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Админ контроллер для журнала ART транзакций
 */
@RestController
@RequestMapping("/api/admin/art-transactions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "ART Транзакции (Admin)", description = "Журнал ART транзакций всех пользователей (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class ArtTransactionAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtTransactionAdminController.class);

    private final ArtRewardService artRewardService;

    @Autowired
    public ArtTransactionAdminController(ArtRewardService artRewardService) {
        this.artRewardService = artRewardService;
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
}

package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.transaction.TonTransactionDto;
import com.example.sticker_art_gallery.model.transaction.BlockchainTransactionEntity;
import com.example.sticker_art_gallery.repository.transaction.BlockchainTransactionRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Админ контроллер для журнала TON транзакций
 */
@RestController
@RequestMapping("/api/admin/ton-transactions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "TON Транзакции (Admin)", description = "Журнал TON транзакций всех пользователей (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class TonTransactionAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TonTransactionAdminController.class);

    private final BlockchainTransactionRepository blockchainTransactionRepository;

    @Autowired
    public TonTransactionAdminController(BlockchainTransactionRepository blockchainTransactionRepository) {
        this.blockchainTransactionRepository = blockchainTransactionRepository;
    }

    /**
     * Получить все TON транзакции с пагинацией
     */
    @GetMapping
    @Operation(
            summary = "Получить все TON транзакции",
            description = "Возвращает журнал всех TON транзакций с пагинацией (только для ADMIN)"
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
    public ResponseEntity<PageResponse<TonTransactionDto>> getAllTransactions(
            @ParameterObject @Valid PageRequest pageRequest) {
        try {
            // Создаем Pageable с сортировкой по дате (новые первыми)
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                    pageRequest.getPage(),
                    pageRequest.getSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
            
            Page<BlockchainTransactionEntity> transactions = blockchainTransactionRepository.findAll(pageable);
            PageResponse<TonTransactionDto> response = PageResponse.of(
                    transactions.map(TonTransactionDto::fromEntity)
            );
            LOGGER.info("💎 Возвращено {} TON транзакций", response.getContent().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка TON транзакций: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

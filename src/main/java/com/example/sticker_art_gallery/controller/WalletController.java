package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.wallet.LinkWalletRequest;
import com.example.sticker_art_gallery.dto.wallet.WalletDto;
import com.example.sticker_art_gallery.model.transaction.UserWalletEntity;
import com.example.sticker_art_gallery.service.transaction.WalletService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с кошельками пользователей
 */
@RestController
@RequestMapping("/api/wallets")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Кошельки", description = "Управление TON-кошельками пользователей")
@SecurityRequirement(name = "TelegramInitData")
public class WalletController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Привязать TON-кошелёк к текущему пользователю
     * Деактивирует все старые активные кошельки пользователя
     */
    @PostMapping("/link")
    @Operation(
        summary = "Привязать кошелёк",
        description = "Привязывает TON-кошелёк к текущему пользователю. Все старые активные кошельки деактивируются."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Кошелёк успешно привязан",
            content = @Content(schema = @Schema(implementation = WalletDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "walletAddress": "EQD...",
                        "walletType": null,
                        "isActive": true,
                        "createdAt": "2024-01-01T12:00:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные (неверный формат адреса)"),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    public ResponseEntity<WalletDto> linkWallet(@Valid @RequestBody LinkWalletRequest request) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Не удалось определить ID текущего пользователя");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            LOGGER.info("Привязка кошелька: userId={}, address={}, type={}", 
                       currentUserId, request.getWalletAddress(), request.getWalletType());

            UserWalletEntity wallet = walletService.linkWallet(
                    currentUserId,
                    request.getWalletAddress(),
                    request.getWalletType()
            );

            WalletDto dto = toDto(wallet);
            LOGGER.info("✅ Кошелёк привязан: walletId={}", wallet.getId());
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при привязке кошелька: {}", e.getMessage());
            throw e; // Обрабатывается ValidationExceptionHandler
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при привязке кошелька: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить активный кошелёк текущего пользователя
     */
    @GetMapping("/my")
    @Operation(
        summary = "Получить мой кошелёк",
        description = "Возвращает активный TON-кошелёк текущего пользователя или null, если кошелёк не привязан"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Кошелёк найден или не привязан",
            content = @Content(schema = @Schema(implementation = WalletDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "walletAddress": "EQD...",
                        "walletType": null,
                        "isActive": true,
                        "createdAt": "2024-01-01T12:00:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    public ResponseEntity<WalletDto> getMyWallet() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Не удалось определить ID текущего пользователя");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            try {
                UserWalletEntity wallet = walletService.getActiveWallet(currentUserId);
                WalletDto dto = toDto(wallet);
                return ResponseEntity.ok(dto);
            } catch (com.example.sticker_art_gallery.exception.WalletNotFoundException e) {
                // Кошелёк не привязан - возвращаем null
                return ResponseEntity.ok(null);
            }

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении кошелька: {}", e.getMessage(), e);
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

    /**
     * Преобразовать Entity в DTO
     */
    private WalletDto toDto(UserWalletEntity wallet) {
        return new WalletDto(
                wallet.getId(),
                wallet.getWalletAddress(),
                wallet.getWalletType(),
                wallet.getIsActive(),
                wallet.getCreatedAt()
        );
    }
}


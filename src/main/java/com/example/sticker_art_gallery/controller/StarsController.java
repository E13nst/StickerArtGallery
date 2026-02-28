package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.payment.*;
import com.example.sticker_art_gallery.service.payment.StarsPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер для работы с покупками ART за Telegram Stars
 */
@RestController
@RequestMapping("/api/stars")
@Tag(name = "Telegram Stars", description = "Покупка ART за Telegram Stars")
@SecurityRequirement(name = "TelegramInitData")
public class StarsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarsController.class);

    private final StarsPaymentService starsPaymentService;

    @Value("${app.url}")
    private String appUrl;
    
    @Value("${app.stickerbot.api-url}")
    private String stickerBotApiUrl;

    @Autowired
    public StarsController(StarsPaymentService starsPaymentService) {
        this.starsPaymentService = starsPaymentService;
    }

    /**
     * Получить список активных тарифных пакетов
     */
    @GetMapping("/packages")
    @Operation(
            summary = "Получить список активных тарифных пакетов",
            description = "Возвращает список всех активных тарифных пакетов для покупки ART за Stars (публичный endpoint)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пакетов получен",
                    content = @Content(
                            schema = @Schema(implementation = StarsPackageDto.class),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": 1,
                                        "code": "STARTER",
                                        "name": "Starter Pack",
                                        "description": "100 ART баллов",
                                        "starsPrice": 50,
                                        "artAmount": 100,
                                        "sortOrder": 1,
                                        "createdAt": "2025-01-15T10:00:00Z"
                                      },
                                      {
                                        "id": 2,
                                        "code": "BASIC",
                                        "name": "Basic Pack",
                                        "description": "250 ART баллов",
                                        "starsPrice": 100,
                                        "artAmount": 250,
                                        "sortOrder": 2,
                                        "createdAt": "2025-01-15T10:00:00Z"
                                      }
                                    ]
                                    """)
                    )
            )
    })
    public ResponseEntity<List<StarsPackageDto>> getPackages() {
        try {
            List<StarsPackageDto> packages = starsPaymentService.getActivePackages();
            LOGGER.info("📦 Возвращено {} активных пакетов", packages.size());
            return ResponseEntity.ok(packages);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка пакетов: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Создать invoice для покупки пакета ART за Stars
     */
    @PostMapping("/create-invoice")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Создать invoice для оплаты Stars",
            description = "Создает invoice для выбранного пакета и возвращает URL для открытия оплаты в Telegram"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice успешно создан",
                    content = @Content(
                            schema = @Schema(implementation = CreateInvoiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "invoiceUrl": "https://t.me/$abcdef1234567890ABCDEF",
                                      "intentId": 123,
                                      "starsPackage": {
                                        "id": 2,
                                        "code": "BASIC",
                                        "name": "Basic Pack",
                                        "description": "250 ART баллов",
                                        "starsPrice": 100,
                                        "artAmount": 250,
                                        "sortOrder": 2,
                                        "createdAt": "2025-01-15T10:00:00Z"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Пакет не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<CreateInvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestHeader(value = "X-Telegram-Init-Data", required = false) String telegramInitData) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warn("⚠️ Попытка создать invoice без авторизации");
                return ResponseEntity.status(403).build();
            }

            CreateInvoiceResponse response = starsPaymentService.createInvoice(userId, request, telegramInitData);
            LOGGER.info("🧾 Создан invoice: userId={}, packageCode={}, intentId={}",
                    userId, request.getPackageCode(), response.getIntentId());
            return ResponseEntity.ok(response);
        } catch (java.util.NoSuchElementException e) {
            LOGGER.warn("⚠️ Пакет не найден при создании invoice: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при создании invoice: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании invoice для packageCode={}: {}",
                    request.getPackageCode(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить историю покупок текущего пользователя
     */
    @GetMapping("/purchases")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Получить историю покупок",
            description = "Возвращает историю покупок ART за Stars текущего пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "История покупок получена",
                    content = @Content(
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "content": [
                                        {
                                          "id": 1,
                                          "packageCode": "STARTER",
                                          "packageName": "Starter Pack",
                                          "starsPaid": 50,
                                          "artCredited": 100,
                                          "createdAt": "2025-01-15T12:00:00Z"
                                        }
                                      ],
                                      "page": 0,
                                      "size": 20,
                                      "totalElements": 1,
                                      "totalPages": 1,
                                      "first": true,
                                      "last": true,
                                      "hasNext": false,
                                      "hasPrevious": false
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StarsPurchaseDto>> getPurchaseHistory(
            @ParameterObject @Valid PageRequest pageRequest) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warn("⚠️ Попытка получить историю покупок без авторизации");
                return ResponseEntity.status(403).build();
            }

            var page = starsPaymentService.getPurchaseHistory(userId, pageRequest.toPageable());
            PageResponse<StarsPurchaseDto> response = PageResponse.of(page);
            LOGGER.info("📜 Возвращено {} покупок для пользователя {}", response.getContent().size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении истории покупок: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить детали покупки по ID
     */
    @GetMapping("/purchases/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Получить детали покупки",
            description = "Возвращает детали конкретной покупки по ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Детали покупки получены",
                    content = @Content(schema = @Schema(implementation = StarsPurchaseDto.class))
            ),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Покупка не найдена"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StarsPurchaseDto> getPurchase(
            @Parameter(description = "ID покупки", required = true, example = "1")
            @PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(403).build();
            }

            // TODO: Реализовать получение покупки по ID с проверкой принадлежности пользователю
            // Пока возвращаем 404
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении покупки {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить конфигурацию для интеграции Stars payments в frontend
     */
    @GetMapping("/config")
    @Operation(
            summary = "Получить конфигурацию для оплаты Stars",
            description = "Возвращает URL внешнего StickerBot API и URL webhook для backend. " +
                    "Используется frontend для динамического получения конфигурации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Конфигурация получена",
                    content = @Content(
                            schema = @Schema(implementation = StarsConfigDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "botApiUrl": "https://stixly-e13nst.amvera.io",
                                      "webhookUrl": "https://your-backend.com/api/internal/webhooks/stars-payment"
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<StarsConfigDto> getConfig() {
        try {
            String webhookUrl = appUrl + "/api/internal/webhooks/stars-payment";
            
            StarsConfigDto config = StarsConfigDto.of(stickerBotApiUrl, webhookUrl);
            LOGGER.debug("⚙️ Возвращена конфигурация Stars: botApiUrl={}, webhookUrl={}", 
                    stickerBotApiUrl, webhookUrl);
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении конфигурации: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить последнюю покупку текущего пользователя
     */
    @GetMapping("/purchases/recent")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Получить последнюю покупку",
            description = "Возвращает последнюю покупку ART за Stars текущего пользователя. " +
                    "Используется для проверки статуса после оплаты."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Последняя покупка получена",
                    content = @Content(
                            schema = @Schema(implementation = StarsPurchaseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 123,
                                      "packageCode": "BASIC",
                                      "packageName": "Basic Pack",
                                      "starsPaid": 100,
                                      "artCredited": 250,
                                      "createdAt": "2025-02-06T12:00:00Z"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Покупок не найдено"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StarsPurchaseDto> getRecentPurchase() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warn("⚠️ Попытка получить последнюю покупку без авторизации");
                return ResponseEntity.status(403).build();
            }

            Optional<StarsPurchaseDto> recentPurchase = starsPaymentService.getRecentPurchase(userId);
            
            if (recentPurchase.isEmpty()) {
                LOGGER.debug("📭 Нет покупок для пользователя {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.info("📦 Возвращена последняя покупка для пользователя {}: purchaseId={}", 
                    userId, recentPurchase.get().getId());
            return ResponseEntity.ok(recentPurchase.get());
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении последней покупки: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Извлечь ID текущего пользователя из SecurityContext
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }
}

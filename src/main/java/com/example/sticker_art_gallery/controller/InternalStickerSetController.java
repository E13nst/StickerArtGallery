package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Межсервисный контроллер для создания стикерсетов от имени пользователей.
 */
@RestController
@RequestMapping("/internal/stickersets")
@Tag(name = "Internal Sticker Sets", description = "Эндпоинты для межсервисного создания стикерсетов в галерее")
@SecurityRequirement(name = "ServiceToken")
@Validated
public class InternalStickerSetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalStickerSetController.class);

    private final StickerSetService stickerSetService;

    public InternalStickerSetController(StickerSetService stickerSetService) {
        this.stickerSetService = stickerSetService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Создать стикерсет от имени пользователя",
        description = """
            Межсервисный эндпоинт для регистрации стикерсета Telegram в галерее.
            Токен сервиса должен быть передан в заголовке `X-Service-Token`.
            """,
        parameters = {
            @Parameter(
                name = "userId",
                in = ParameterIn.QUERY,
                required = true,
                description = "Telegram ID пользователя, от имени которого создаётся стикерсет",
                example = "123456789"
            ),
            @Parameter(
                name = "language",
                in = ParameterIn.QUERY,
                description = "Язык сообщений об ошибках (`ru` или `en`). По умолчанию `en`.",
                example = "en"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Стикерсет успешно создан",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
        @ApiResponse(responseCode = "401", description = "Межсервисная авторизация не пройдена"),
        @ApiResponse(responseCode = "403", description = "Нет прав для выполнения операции"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(
            schema = @Schema(implementation = CreateStickerSetDto.class),
            examples = @ExampleObject(value = """
                {
                  "name": "https://t.me/addstickers/my_pack_by_bot",
                  "title": "Мои стикеры",
                  "categoryKeys": ["animals", "cute"],
                  "isPublic": true
                }
                """)
        )
    )
    public ResponseEntity<?> createStickerSetForUser(
            @Valid @RequestBody CreateStickerSetDto createDto,
            @RequestParam @NotNull @Positive Long userId,
            @RequestParam(required = false) String language,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo) {

        if (createDto.getIsPublic() == null) {
            createDto.setIsPublic(true);
        }

        try {
            LOGGER.info("🤝 Межсервисное создание стикерсета для userId {}: {}", userId, createDto.getName());
            StickerSet stickerSet = stickerSetService.createStickerSetForUser(createDto, userId, language);
            String responseLanguage = (language == null || language.isBlank()) ? "en" : language;
            StickerSetDto responseDto = stickerSetService.findByIdWithBotApiData(stickerSet.getId(), responseLanguage, userId, shortInfo);
            if (responseDto == null) {
                responseDto = StickerSetDto.fromEntity(stickerSet, responseLanguage, userId);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("⚠️ Ошибка валидации при межсервисном создании стикерсета: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Validation error",
                            "message", ex.getMessage()
                    ));
        } catch (Exception ex) {
            LOGGER.error("❌ Внутренняя ошибка при межсервисном создании стикерсета", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal error",
                            "message", "Unexpected error while creating stickerset"
                    ));
        }
    }
}


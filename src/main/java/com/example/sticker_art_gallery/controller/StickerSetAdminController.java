package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * Контроллер для админских операций со стикерсетами
 */
@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*")
@Tag(name = "Админские операции со стикерсетами", description = "Управление официальным статусом и авторами стикерсетов (только для админов)")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetAdminController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetAdminController.class);
    private final StickerSetService stickerSetService;
    
    @Autowired
    public StickerSetAdminController(StickerSetService stickerSetService) {
        this.stickerSetService = stickerSetService;
    }
    
    /**
     * Отметить стикерсет как официальный (только для админа)
     */
    @PutMapping("/{id}/official")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Отметить как официальный",
        description = "Устанавливает флаг isOfficial=true для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет отмечен как официальный",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может изменять официальный статус"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> markStickerSetOfficial(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🏅 Установка официального статуса для стикерсета {}", id);
            StickerSet updated = stickerSetService.setOfficial(id);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Не найдено",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при установке официального статуса стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Снять признак официального стикерсета (только для админа)
     */
    @PutMapping("/{id}/unofficial")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Снять официальный статус",
        description = "Устанавливает флаг isOfficial=false для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет отмечен как неофициальный",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может изменять официальный статус"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> markStickerSetUnofficial(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🏷️ Снятие официального статуса для стикерсета {}", id);
            StickerSet updated = stickerSetService.unsetOfficial(id);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Не найдено",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при снятии официального статуса стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Установить автора стикерсета (только для админа)
     */
    @PutMapping("/{id}/author")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Установить автора стикерсета",
        description = "Устанавливает Telegram ID автора (authorId) для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Автор установлен",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = "{\"authorId\":123456789}"))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может устанавливать автора"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> setStickerSetAuthor(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Тело запроса с authorId", required = true)
            @RequestBody Map<String, Long> request) {
        try {
            if (request == null || !request.containsKey("authorId")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ошибка валидации",
                    "message", "Поле authorId обязательно"
                ));
            }
            Long authorId = request.get("authorId");
            StickerSet updated = stickerSetService.setAuthor(id, authorId);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Ошибка валидации",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при установке автора для стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Очистить автора стикерсета (только для админа)
     */
    @DeleteMapping("/{id}/author")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Очистить автора стикерсета",
        description = "Очищает Telegram ID автора (authorId=null) для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Автор очищен",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может очищать автора"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> clearStickerSetAuthor(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            StickerSet updated = stickerSetService.clearAuthor(id);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Не найдено",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при очистке автора стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

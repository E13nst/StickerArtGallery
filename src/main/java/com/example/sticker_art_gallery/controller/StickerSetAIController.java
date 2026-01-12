package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.CategorySuggestionResult;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.ai.AutoCategorizationService;
import com.example.sticker_art_gallery.service.ai.StickerSetDescriptionService;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Контроллер для AI-функций стикерсетов
 */
@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*")
@Tag(name = "AI функции стикерсетов", description = "AI-функции для стикерсетов: предложение категорий и генерация описаний")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetAIController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetAIController.class);
    private final StickerSetService stickerSetService;
    private final AutoCategorizationService autoCategorizationService;
    private final StickerSetDescriptionService stickerSetDescriptionService;
    private final StickerSetControllerHelper helper;
    
    @Autowired
    public StickerSetAIController(StickerSetService stickerSetService,
                                 AutoCategorizationService autoCategorizationService,
                                 StickerSetDescriptionService stickerSetDescriptionService,
                                 StickerSetControllerHelper helper) {
        this.stickerSetService = stickerSetService;
        this.autoCategorizationService = autoCategorizationService;
        this.stickerSetDescriptionService = stickerSetDescriptionService;
        this.helper = helper;
    }
    
    /**
     * Предложить категории для стикерсета (предпросмотр или применение)
     */
    @PostMapping("/{id}/ai/suggest-categories")
    @Operation(
        summary = "Предложить категории для стикерсета",
        description = "Использует AI (ChatGPT) для анализа title стикерсета и предложения наиболее подходящих категорий. " +
                     "С параметром apply=false возвращает только предпросмотр, с apply=true - применяет категории. " +
                     "Параметр minConfidence задает минимальный уровень уверенности (0.0-1.0) для применения категорий " +
                     "при apply=true. При apply=false этот параметр не имеет значения. " +
                     "Доступно владельцу стикерсета или администратору. " +
                     "Для работы требуется переменная окружения OPENAI_API_KEY."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Категории успешно предложены",
            content = @Content(schema = @Schema(implementation = CategorySuggestionResult.class),
                examples = @ExampleObject(value = """
                    {
                        "analyzedTitle": "Cute Cats",
                        "suggestedCategories": [
                            {
                                "categoryKey": "animals",
                                "categoryName": "Животные",
                                "confidence": 0.95,
                                "reason": "Contains cat-related imagery"
                            },
                            {
                                "categoryKey": "cute",
                                "categoryName": "Милые",
                                "confidence": 0.87,
                                "reason": "Title explicitly mentions cute theme"
                            }
                        ]
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или стикерсет без title"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно категоризовать только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или ошибка при работе с AI")
    })
    public ResponseEntity<CategorySuggestionResult> suggestCategoriesForStickerSet(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Применить категории (true) или только предпросмотр (false)", example = "false")
            @RequestParam(defaultValue = "false") boolean apply,
            @Parameter(description = "Минимальный уровень уверенности (0.0-1.0) для применения категорий при apply=true. " +
                                    "Категории с confidence ниже этого значения не будут применены. " +
                                    "При apply=false этот параметр не имеет значения.", example = "0.8")
            @RequestParam(required = false) 
            @DecimalMin(value = "0.0", message = "minConfidence должен быть >= 0.0")
            @DecimalMax(value = "1.0", message = "minConfidence должен быть <= 1.0")
            Double minConfidence,
            HttpServletRequest request) {
        try {
            // Валидация minConfidence (если указан)
            if (minConfidence != null && (minConfidence < 0.0 || minConfidence > 1.0)) {
                LOGGER.warn("⚠️ Некорректное значение minConfidence: {} (должно быть от 0.0 до 1.0)", minConfidence);
                return ResponseEntity.badRequest().body(null);
            }
            
            String language = helper.getLanguageFromHeaderOrUser(request);
            LOGGER.info("🤖 Предложение категорий для стикерсета ID: {}, apply={}, minConfidence={}", 
                id, apply, minConfidence);
            
            // Проверка прав доступа (владелец или админ)
            Long currentUserId = helper.getCurrentUserId();
            StickerSet stickerSet = stickerSetService.findById(id);
            if (stickerSet == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!helper.isOwnerOrAdmin(stickerSet.getUserId(), currentUserId)) {
                LOGGER.warn("⚠️ Пользователь {} попытался категоризовать чужой стикерсет {}", currentUserId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CategorySuggestionResult result = autoCategorizationService.suggestCategoriesForStickerSet(
                id, apply, language, minConfidence);
            
            LOGGER.info("✅ Категории для стикерсета {} предложены (apply={}, применено: {})", 
                id, apply, apply ? result.getSuggestedCategories().size() : "N/A");
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректные данные для предложения категорий стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при предложении категорий стикерсета {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Сгенерировать описание стикерсета с помощью AI
     */
    @PostMapping("/{id}/ai/generate-description")
    @Operation(
        summary = "Сгенерировать описание стикерсета с помощью AI",
        description = "Использует AI (ChatGPT) для анализа изображения стикерсета и генерации описаний на русском и английском языках. " +
                     "Описания сохраняются в отдельную таблицу для поддержки множества языков. " +
                     "Доступно владельцу стикерсета или администратору. " +
                     "Для работы требуется переменная окружения OPENAI_API_KEY и доступность сервиса sticker-processor."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Описания успешно сгенерированы",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "ru": "Коллекция милых котиков с забавными выражениями",
                        "en": "Collection of cute cats with funny expressions"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно генерировать описания только для своих стикерсетов"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или ошибка при работе с AI/sticker-processor")
    })
    public ResponseEntity<Map<String, String>> generateDescriptionForStickerSet(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            HttpServletRequest request) {
        try {
            LOGGER.info("🤖 Генерация описания для стикерсета ID: {}", id);
            
            // Проверка прав доступа (владелец или админ)
            Long currentUserId = helper.getCurrentUserId();
            StickerSet stickerSet = stickerSetService.findById(id);
            if (stickerSet == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!helper.isOwnerOrAdmin(stickerSet.getUserId(), currentUserId)) {
                LOGGER.warn("⚠️ Пользователь {} попытался сгенерировать описание для чужого стикерсета {}", currentUserId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Генерируем описания
            Map<String, String> descriptions = stickerSetDescriptionService.generateDescriptionForStickerSet(id, currentUserId);
            
            LOGGER.info("✅ Описания для стикерсета {} успешно сгенерированы", id);
            return ResponseEntity.ok(descriptions);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректные данные для генерации описания стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при генерации описания стикерсета {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

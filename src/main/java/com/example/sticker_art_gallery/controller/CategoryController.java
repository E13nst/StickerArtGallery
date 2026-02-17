package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.*;
import com.example.sticker_art_gallery.service.category.CategoryService;
import com.example.sticker_art_gallery.service.user.UserService;
import com.example.sticker_art_gallery.service.ai.AutoCategorizationService;
import com.example.sticker_art_gallery.service.ai.AIService;
import com.example.sticker_art_gallery.model.user.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST контроллер для управления категориями стикерсетов
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API для управления категориями стикерсетов")
public class CategoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryController.class);
    
    private final CategoryService categoryService;
    private final UserService userService;
    private final AutoCategorizationService autoCategorizationService;
    private final AIService aiService;

    @GetMapping
    @Operation(
        summary = "Получить все активные категории",
        description = "Возвращает список всех активных категорий с локализацией через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
                     "Категории возвращаются отсортированными по displayOrder. " +
                     "Поддерживает русский и английский языки для названий и описаний."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список категорий успешно получен",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<CategoryDto>> getAllCategories(HttpServletRequest request) {
        String language = getLanguageFromHeaderOrUser(request);
        List<CategoryDto> categories = categoryService.getAllActiveCategories(language);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/counts")
    @Operation(
        summary = "Получить активные категории с количеством стикерсетов",
        description = "Возвращает список активных категорий с количеством публичных и не заблокированных стикерсетов в каждой. " +
                     "Поддерживает фильтры: officialOnly, authorId (deprecated), isVerified."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список категорий с количеством успешно получен",
            content = @Content(schema = @Schema(implementation = CategoryWithCountDto.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<CategoryWithCountDto>> getCategoriesWithCounts(
            @Parameter(description = "Показывать только официальные стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean officialOnly,
            @Parameter(description = "Фильтр по автору (deprecated: интерпретируется как userId=authorId, isVerified=true)", example = "123456789", deprecated = true)
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Показывать только верифицированные стикерсеты (isVerified=true)", example = "false")
            @RequestParam(required = false) Boolean isVerified,
            HttpServletRequest request) {
        String language = getLanguageFromHeaderOrUser(request);
        Long effectiveUserId = authorId != null ? authorId : null;
        Boolean effectiveIsVerified = authorId != null ? Boolean.TRUE : isVerified;
        List<CategoryWithCountDto> result = categoryService.getActiveCategoriesWithCounts(language, officialOnly, effectiveUserId, effectiveIsVerified);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Получить категорию по ключу",
        description = "Возвращает информацию о категории по её уникальному ключу. " +
                     "Ключ должен содержать только латинские буквы, цифры и подчеркивания. " +
                     "Поддерживает локализацию названий и описаний через заголовок X-Language (ru/en) или автоматически из initData пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категория найдена",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<CategoryDto> getCategoryByKey(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key,
            HttpServletRequest request
    ) {
        try {
            String language = getLanguageFromHeaderOrUser(request);
            CategoryDto category = categoryService.getCategoryByKey(key, language);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(
        summary = "Создать новую категорию",
        description = "Создает новую категорию с поддержкой локализации. " +
                     "Поле 'key' является обязательным и должно быть уникальным. " +
                     "Поддерживает создание категорий с названиями и описаниями на русском и английском языках. " +
                     "Язык ответа определяется через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
                     "displayOrder определяет порядок отображения в списке категорий."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Категория успешно создана",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или категория уже существует"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> createCategory(
            @Valid @RequestBody CreateCategoryDto createDto,
            HttpServletRequest request
    ) {
        try {
            String language = getLanguageFromHeaderOrUser(request);
            CategoryDto category = categoryService.createCategory(createDto, language);
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Обновить категорию",
        description = "Обновляет существующую категорию. Требуется авторизация. " +
                     "Язык ответа определяется через заголовок X-Language (ru/en) или автоматически из initData пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категория успешно обновлена"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> updateCategory(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key,
            @Valid @RequestBody UpdateCategoryDto updateDto,
            HttpServletRequest request
    ) {
        try {
            String language = getLanguageFromHeaderOrUser(request);
            CategoryDto category = categoryService.updateCategory(key, updateDto, language);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{key}")
    @Operation(
        summary = "Деактивировать категорию",
        description = "Деактивирует категорию (мягкое удаление). Требуется авторизация."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Категория успешно деактивирована"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> deactivateCategory(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key
    ) {
        try {
            categoryService.deactivateCategory(key);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{key}/count")
    @Operation(
        summary = "Получить количество стикерсетов в категории",
        description = "Возвращает количество стикерсетов, привязанных к категории"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Количество успешно получено"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getStickerSetCount(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key
    ) {
        try {
            long count = categoryService.getStickerSetCount(key);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    
    /**
     * Предложить категории для произвольного title (метод #1)
     */
    @GetMapping("/ai/suggest")
    @Operation(
        summary = "Предложить категории для заголовка стикерсета",
        description = "Использует AI для анализа произвольного title и предложения подходящих категорий из существующих. " +
                     "Не требует наличия стикерсета в базе. Результаты кешируются на 1 час."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категории успешно предложены",
            content = @Content(schema = @Schema(implementation = CategorySuggestionResult.class))),
        @ApiResponse(responseCode = "400", description = "Title не указан или пустой"),
        @ApiResponse(responseCode = "500", description = "Ошибка при работе с AI")
    })
    public ResponseEntity<CategorySuggestionResult> suggestCategoriesForTitle(
            @Parameter(description = "Заголовок стикерсета для анализа", example = "Cute Cats", required = true)
            @RequestParam String title,
            HttpServletRequest request) {
        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            String language = getLanguageFromHeaderOrUser(request);
            LOGGER.info("🤖 Запрос предложения категорий для title: '{}'", title);
            
            CategorySuggestionResult result = autoCategorizationService.suggestCategoriesForTitle(title, language);
            
            LOGGER.info("✅ Предложено {} категорий для title '{}'", result.getSuggestedCategories().size(), title);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при предложении категорий для title: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    
    /**
     * Извлечь ID текущего пользователя из SecurityContext (может вернуть null)
     */
    private Long getCurrentUserIdOrNull() {
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
    
    /**
     * Проверка, является ли пользователь админом
     */
    private boolean isAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке прав администратора: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Извлечь язык из заголовка X-Language или из initData пользователя
     * @param request HTTP запрос для получения заголовков
     * @return код языка (ru/en), по умолчанию "en"
     */
    private String getLanguageFromHeaderOrUser(HttpServletRequest request) {
        // Сначала проверяем заголовок X-Language
        String languageFromHeader = request.getHeader("X-Language");
        if (languageFromHeader != null && !languageFromHeader.trim().isEmpty()) {
            String lang = languageFromHeader.trim().toLowerCase();
            if ("ru".equals(lang) || "en".equals(lang)) {
                return lang;
            }
        }
        
        // Если заголовок не указан или некорректный, пытаемся получить из initData пользователя
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId != null) {
            try {
                java.util.Optional<UserEntity> userOpt = userService.findById(currentUserId);
                if (userOpt.isPresent()) {
                    String userLanguage = userOpt.get().getLanguageCode();
                    if (userLanguage != null && !userLanguage.trim().isEmpty()) {
                        String lang = userLanguage.trim().toLowerCase();
                        if ("ru".equals(lang) || "en".equals(lang)) {
                            return lang;
                        }
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
        
        // По умолчанию возвращаем английский
        return "en";
    }
    
    /**
     * Тестовый эндпоинт для проверки подключения к ChatGPT (GET с параметрами)
     * Доступен только для администраторов
     */
    @GetMapping("/ai/test-chatgpt")
    @Operation(
        summary = "Тест подключения к ChatGPT (GET)",
        description = "Простой тест для проверки работоспособности подключения к OpenAI ChatGPT. " +
                     "Принимает message и prompt через параметры запроса, возвращает ответ от AI. " +
                     "Доступен только для администраторов."
    )
    @ApiResponse(responseCode = "200", description = "Ответ от ChatGPT получен",
        content = @Content(schema = @Schema(implementation = Map.class)))
    @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN")
    @ApiResponse(responseCode = "500", description = "Ошибка при вызове ChatGPT")
    public ResponseEntity<Map<String, Object>> testChatGPTGet(
            @Parameter(description = "Сообщение пользователя для AI", example = "Привет, как дела?")
            @RequestParam(required = false, defaultValue = "Привет! Как дела?") String message,
            @Parameter(description = "Системный промпт для AI", example = "Ты дружелюбный помощник")
            @RequestParam(required = false, defaultValue = "Ты дружелюбный помощник.") String prompt) {
        
        // Проверка прав администратора
        if (!isAdmin()) {
            LOGGER.warn("⚠️ Попытка доступа к тестовому эндпоинту ChatGPT без прав администратора");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Forbidden", "message", "Доступ разрешен только администраторам"));
        }
        
        return testChatGPT(message, prompt);
    }
    
    /**
     * Общий метод для тестирования ChatGPT
     */
    private ResponseEntity<Map<String, Object>> testChatGPT(String message, String prompt) {
        LOGGER.info("🧪 Тест подключения к ChatGPT | message length: {} chars, prompt length: {} chars", 
            message != null ? message.length() : 0, prompt != null ? prompt.length() : 0);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String conversationId = "test-chatgpt-" + System.currentTimeMillis();
            String aiResponse = aiService.completion(conversationId, message, prompt, null);
            
            response.put("success", true);
            response.put("message", "Ответ от AI получен успешно");
            response.put("response", aiResponse);
            response.put("responseLength", aiResponse != null ? aiResponse.length() : 0);
            response.put("conversationId", conversationId);
            response.put("timestamp", java.time.Instant.now().toString());
            
            LOGGER.info("✅ Тест ChatGPT успешен | response length: {} chars", aiResponse != null ? aiResponse.length() : 0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при тесте ChatGPT: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Ошибка при вызове ChatGPT");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}


package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.generation.*;
import com.example.sticker_art_gallery.service.generation.StickerGenerationService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generation")
@CrossOrigin(origins = "*")
@Tag(name = "Генерация стикеров", description = "API для генерации стикеров через WaveSpeed")
@SecurityRequirement(name = "TelegramInitData")
public class StickerGenerationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerGenerationController.class);

    private final StickerGenerationService generationService;

    @Autowired
    public StickerGenerationController(StickerGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Запустить генерацию стикера",
        description = "Запускает асинхронную генерацию стикера по промпту. Списывает ART-баллы и возвращает task_id для отслеживания статуса."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Генерация запущена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GenerateStickerResponse.class),
                examples = @ExampleObject(
                    name = "Успешный запуск",
                    value = """
                        {
                          "taskId": "abc123-def456-ghi789"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные"),
        @ApiResponse(responseCode = "402", description = "Недостаточно ART-баллов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<GenerateStickerResponse> generate(@Valid @RequestBody GenerateStickerRequest request) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String taskId = generationService.startGeneration(userId, request);
            LOGGER.info("Generation started: taskId={}, userId={}", taskId, userId);
            return ResponseEntity.ok(new GenerateStickerResponse(taskId));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Недостаточно ART")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
            }
            throw e;
        }
    }

    @GetMapping("/status/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Получить статус генерации",
        description = "Возвращает текущий статус задачи генерации и результат (если готов). Админ может просматривать статус любой задачи."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статус получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GenerationStatusResponse.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Задача не найдена"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public ResponseEntity<GenerationStatusResponse> getStatus(
            @Parameter(description = "ID задачи генерации", required = true, example = "abc123-def456-ghi789")
            @PathVariable String taskId) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean isAdmin = isCurrentUserAdmin();
            GenerationStatusResponse response = generationService.getGenerationStatus(taskId, userId, isAdmin);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            throw e;
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Получить историю генераций",
        description = "Возвращает историю генераций пользователя с пагинацией"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "История получена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GenerationHistoryResponse.class)
            )
        )
    })
    public ResponseEntity<GenerationHistoryResponse> getHistory(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        Page<GenerationStatusResponse> history = generationService.getGenerationHistory(
                userId, pageRequest.toPageable());
        
        GenerationHistoryResponse response = new GenerationHistoryResponse(
                history.getContent(),
                history.getNumber(),
                history.getSize(),
                history.getTotalElements(),
                history.getTotalPages(),
                history.isFirst(),
                history.isLast(),
                history.hasNext(),
                history.hasPrevious()
        );

        return ResponseEntity.ok(response);
    }

    private Long extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse userId from authentication name: {}", authentication.getName());
            return null;
        }
    }

    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking admin role: {}", e.getMessage());
        }
        return false;
    }
}

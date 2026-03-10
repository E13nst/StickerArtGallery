package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerResponse;
import com.example.sticker_art_gallery.dto.generation.GenerateStickerV2Request;
import com.example.sticker_art_gallery.dto.generation.GenerationHistoryResponse;
import com.example.sticker_art_gallery.dto.generation.GenerationStatusResponse;
import com.example.sticker_art_gallery.dto.generation.SaveToSetV2Request;
import com.example.sticker_art_gallery.dto.generation.SaveToSetV2Response;
import com.example.sticker_art_gallery.service.generation.StickerGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/generation/v2")
@CrossOrigin(origins = "*")
@Tag(name = "Генерация стикеров v2", description = "API генерации через STICKER_PROCESSOR")
@SecurityRequirement(name = "TelegramInitData")
public class StickerGenerationV2Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerGenerationV2Controller.class);

    private final StickerGenerationService generationService;

    public StickerGenerationV2Controller(StickerGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Запустить генерацию стикера (v2)",
            description = """
                    Запускает генерацию через STICKER_PROCESSOR.
                    Перед отправкой в провайдер применяется текущий pipeline обработки промпта:
                    1) активные Prompt Enhancers пользователя;
                    2) Style Preset по полю stylePresetId (legacy-совместимость со старыми пресетами).
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Генерация запущена"),
            @ApiResponse(responseCode = "400", description = "Неверные входные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<GenerateStickerResponse> generateV2(@Valid @RequestBody GenerateStickerV2Request request) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String taskId = generationService.startGenerationV2(userId, request);
        LOGGER.info("Generation v2 started: taskId={}, userId={}", taskId, userId);
        return ResponseEntity.ok(new GenerateStickerResponse(taskId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "История генераций v2",
            description = "Возвращает только задачи нового v2 flow (generation-v2) с пагинацией."
    )
    public ResponseEntity<GenerationHistoryResponse> getHistoryV2(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        com.example.sticker_art_gallery.dto.PageRequest pageRequest = new com.example.sticker_art_gallery.dto.PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        Page<GenerationStatusResponse> history = generationService.getGenerationHistoryV2(userId, pageRequest.toPageable());
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

    @GetMapping("/status/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить статус генерации (v2)")
    public ResponseEntity<GenerationStatusResponse> getStatusV2(
            @Parameter(description = "ID задачи генерации", required = true) @PathVariable String taskId) {
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

    @PostMapping("/save-to-set")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Сохранить стикер в Telegram set через Sticker Processor (v2)")
    public ResponseEntity<SaveToSetV2Response> saveToSetV2(@Valid @RequestBody SaveToSetV2Request request) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            SaveToSetV2Response response = generationService.saveToSetV2(request);
            int statusCode = Integer.parseInt(response.getStatus());
            return ResponseEntity.status(statusCode).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}

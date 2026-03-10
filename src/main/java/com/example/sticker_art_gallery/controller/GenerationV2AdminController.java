package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.generation.GenerationAdminHistoryItemDto;
import com.example.sticker_art_gallery.service.generation.StickerGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/generation-v2")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Генерации v2 (Админ)", description = "История задач новой генерации generation-v2 (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class GenerationV2AdminController {

    private final StickerGenerationService generationService;

    public GenerationV2AdminController(StickerGenerationService generationService) {
        this.generationService = generationService;
    }

    @GetMapping
    @Operation(
            summary = "Список генераций v2",
            description = "Возвращает только задачи flow=generation-v2 с пагинацией и фильтрами."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список получен",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Невалидный фильтр")
    })
    public ResponseEntity<PageResponse<GenerationAdminHistoryItemDto>> list(
            @Parameter(description = "Telegram ID пользователя", example = "123456789")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Статус задачи (PENDING, PROCESSING_PROMPT, GENERATING, REMOVING_BACKGROUND, COMPLETED, FAILED, TIMEOUT)", example = "COMPLETED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Task ID (точный поиск)", example = "9e2ee0e7-bac5-4bcb-afaa-c10a8391eeeb")
            @RequestParam(required = false) String taskId,
            @Parameter(description = "Номер страницы", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            com.example.sticker_art_gallery.dto.PageRequest pageRequest = new com.example.sticker_art_gallery.dto.PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("createdAt");
            pageRequest.setDirection("DESC");
            Page<GenerationAdminHistoryItemDto> history = generationService.getGenerationHistoryV2ForAdmin(
                    userId, status, taskId, pageRequest.toPageable());
            return ResponseEntity.ok(PageResponse.of(history));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

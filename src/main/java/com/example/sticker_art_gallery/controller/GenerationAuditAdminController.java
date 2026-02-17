package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.generation.GenerationAuditEventDto;
import com.example.sticker_art_gallery.dto.generation.GenerationAuditSessionDto;
import com.example.sticker_art_gallery.service.generation.GenerationAuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/generation-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Лог генерации стикеров (Админ)", description = "Просмотр audit-лога генерации: сессии, этапы, ошибки (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class GenerationAuditAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationAuditAdminController.class);

    private final GenerationAuditQueryService auditQueryService;

    @Autowired
    public GenerationAuditAdminController(GenerationAuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @Operation(
        summary = "Список логов генерации",
        description = "Возвращает список audit-сессий генерации с пагинацией и фильтрами"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class))
        )
    })
    public ResponseEntity<PageResponse<GenerationAuditSessionDto>> getGenerationLogs(
            @Parameter(description = "Telegram ID пользователя") @RequestParam(required = false) Long userId,
            @Parameter(description = "Итоговый статус: COMPLETED, FAILED, TIMEOUT") @RequestParam(required = false) String finalStatus,
            @Parameter(description = "Начало периода (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @Parameter(description = "Конец периода (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @Parameter(description = "Только записи с ошибками") @RequestParam(required = false) Boolean errorOnly,
            @Parameter(description = "Идентификатор задачи (точное совпадение)") @RequestParam(required = false) String taskId,
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        pageRequest.setSort("startedAt");
        pageRequest.setDirection("DESC");
        PageResponse<GenerationAuditSessionDto> response = auditQueryService.findWithFilters(
                userId, finalStatus, dateFrom, dateTo, errorOnly, taskId, pageRequest.toPageable());
        LOGGER.info("📜 Admin generation logs list: page={}, size={}, total={}", page, size, response.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}")
    @Operation(
        summary = "Детали сессии генерации",
        description = "Возвращает одну audit-сессию по taskId"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Сессия найдена", content = @Content(schema = @Schema(implementation = GenerationAuditSessionDto.class))),
        @ApiResponse(responseCode = "404", description = "Сессия не найдена")
    })
    public ResponseEntity<GenerationAuditSessionDto> getGenerationLogDetail(
            @Parameter(description = "Идентификатор задачи", required = true) @PathVariable String taskId) {
        GenerationAuditSessionDto dto = auditQueryService.getByTaskId(taskId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{taskId}/events")
    @Operation(
        summary = "Таймлайн событий генерации",
        description = "Возвращает список событий по этапам pipeline для данной задачи"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список событий получен", content = @Content(schema = @Schema(implementation = GenerationAuditEventDto.class)))
    })
    public ResponseEntity<List<GenerationAuditEventDto>> getGenerationLogEvents(
            @Parameter(description = "Идентификатор задачи", required = true) @PathVariable String taskId) {
        List<GenerationAuditEventDto> events = auditQueryService.getEventsByTaskId(taskId);
        return ResponseEntity.ok(events);
    }
}

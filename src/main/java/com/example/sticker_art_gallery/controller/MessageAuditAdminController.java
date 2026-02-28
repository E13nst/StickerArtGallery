package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.messaging.MessageAuditEventDto;
import com.example.sticker_art_gallery.dto.messaging.MessageAuditSessionDto;
import com.example.sticker_art_gallery.service.messaging.MessageAuditQueryService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/message-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Лог отправки сообщений (Админ)", description = "Просмотр audit-лога отправки сообщений через StickerBot API (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class MessageAuditAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageAuditAdminController.class);

    private final MessageAuditQueryService messageAuditQueryService;

    public MessageAuditAdminController(MessageAuditQueryService messageAuditQueryService) {
        this.messageAuditQueryService = messageAuditQueryService;
    }

    @GetMapping
    @Operation(
            summary = "Список логов отправки сообщений",
            description = "Возвращает список audit-сессий отправки сообщений с пагинацией и фильтрами"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список получен",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            )
    })
    public ResponseEntity<PageResponse<MessageAuditSessionDto>> getMessageLogs(
            @Parameter(description = "Telegram ID пользователя") @RequestParam(required = false) Long userId,
            @Parameter(description = "Итоговый статус: SENT, FAILED") @RequestParam(required = false) String finalStatus,
            @Parameter(description = "Начало периода (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @Parameter(description = "Конец периода (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @Parameter(description = "Только записи с ошибками") @RequestParam(required = false) Boolean errorOnly,
            @Parameter(description = "Идентификатор сообщения (точное совпадение)") @RequestParam(required = false) String messageId,
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        pageRequest.setSort("startedAt");
        pageRequest.setDirection("DESC");
        PageResponse<MessageAuditSessionDto> response = messageAuditQueryService.findWithFilters(
                userId, finalStatus, dateFrom, dateTo, errorOnly, messageId, pageRequest.toPageable());
        LOGGER.info("📨 Admin message logs list: page={}, size={}, total={}", page, size, response.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{messageId}")
    @Operation(
            summary = "Детали отправки сообщения",
            description = "Возвращает одну audit-сессию отправки сообщения по messageId"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сессия найдена", content = @Content(schema = @Schema(implementation = MessageAuditSessionDto.class))),
            @ApiResponse(responseCode = "404", description = "Сессия не найдена")
    })
    public ResponseEntity<MessageAuditSessionDto> getMessageLogDetail(
            @Parameter(description = "Идентификатор сообщения", required = true) @PathVariable String messageId) {
        MessageAuditSessionDto dto = messageAuditQueryService.getByMessageId(messageId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{messageId}/events")
    @Operation(
            summary = "Таймлайн событий отправки",
            description = "Возвращает список событий по этапам отправки сообщения"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список событий получен", content = @Content(schema = @Schema(implementation = MessageAuditEventDto.class)))
    })
    public ResponseEntity<List<MessageAuditEventDto>> getMessageLogEvents(
            @Parameter(description = "Идентификатор сообщения", required = true) @PathVariable String messageId) {
        List<MessageAuditEventDto> events = messageAuditQueryService.getEventsByMessageId(messageId);
        return ResponseEntity.ok(events);
    }
}

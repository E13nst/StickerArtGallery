package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.analytics.AnalyticsDashboardResponseDto;
import com.example.sticker_art_gallery.service.analytics.AnalyticsService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Аналитика (Admin)", description = "Дашборд аналитики: KPI, таймсерии и детализации за период (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class AnalyticsAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsAdminController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsAdminController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(
            summary = "Данные дашборда аналитики",
            description = "Возвращает KPI за период, таймсерии по бакетам и детализации (топ пользователей, стикерсетов, рефералы, генерация). Параметры: from, to (ISO-8601), granularity (hour|day|week), tz (часовой пояс, по умолчанию UTC). Максимальный диапазон 365 дней."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные дашборда",
                    content = @Content(schema = @Schema(implementation = AnalyticsDashboardResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Невалидные параметры (даты, диапазон, granularity)"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<AnalyticsDashboardResponseDto> getDashboard(
            @Parameter(description = "Начало периода (ISO-8601)", required = true, example = "2025-02-01T00:00:00Z")
            @RequestParam String from,
            @Parameter(description = "Конец периода (ISO-8601)", required = true, example = "2025-02-23T23:59:59Z")
            @RequestParam String to,
            @Parameter(description = "Гранулярность бакетов: hour, day, week", example = "day")
            @RequestParam(defaultValue = "day") String granularity,
            @Parameter(description = "Часовой пояс для бакетов", example = "UTC")
            @RequestParam(required = false, defaultValue = "UTC") String tz) {
        try {
            AnalyticsDashboardResponseDto body = analyticsService.getDashboard(from, to, granularity, tz);
            LOGGER.debug("📊 Dashboard returned for period {} - {}", from, to);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Invalid dashboard params: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Error building analytics dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

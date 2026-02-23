package com.example.sticker_art_gallery.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ GET /api/admin/analytics/dashboard.
 * Содержит KPI за период, таймсерии по бакетам и детализации.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные дашборда аналитики за выбранный период")
public class AnalyticsDashboardResponseDto {

    @Schema(description = "Параметры запроса (from, to, granularity)")
    private String from;
    private String to;
    private String granularity;
    private String tz;

    @Schema(description = "KPI-карточки за период")
    private DashboardKpiDto kpiCards;

    @Schema(description = "Таймсерии для графиков")
    private DashboardTimeseriesDto timeseries;

    @Schema(description = "Детализации (топы, группировки)")
    private DashboardBreakdownsDto breakdowns;
}

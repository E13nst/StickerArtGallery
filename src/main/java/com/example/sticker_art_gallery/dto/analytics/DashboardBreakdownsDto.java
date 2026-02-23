package com.example.sticker_art_gallery.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Детализации для дашборда: топы и группировки.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Детализации по блокам аналитики")
public class DashboardBreakdownsDto {

    @Schema(description = "Топ пользователей по активности за период (userId, count)")
    private List<Map<String, Object>> topUsers;
    @Schema(description = "Топ стикерсетов по лайкам за период")
    private List<Map<String, Object>> topStickerSets;

    @Schema(description = "Реферальные события по event_type")
    private Map<String, Long> referralByType;
    @Schema(description = "Генерация по stage и status (ключ: stage_status, значение: count)")
    private Map<String, Long> generationByStageStatus;
}

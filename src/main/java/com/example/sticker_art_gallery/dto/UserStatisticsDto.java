package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO со статистикой по пользователям
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Статистика по пользователям")
public class UserStatisticsDto {

    @Schema(description = "Общее количество пользователей", example = "1250")
    private long total;

    @Schema(description = "Новых пользователей за последние 24 часа", example = "15")
    private long daily;

    @Schema(description = "Новых пользователей за последнюю неделю", example = "98")
    private long weekly;

    @Schema(description = "Активных пользователей за последние 24 часа (ставили лайки или создавали стикерсеты)", example = "45")
    private long activeDaily;

    @Schema(description = "Активных пользователей за последнюю неделю", example = "320")
    private long activeWeekly;
}


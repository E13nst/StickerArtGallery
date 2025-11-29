package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO со статистикой по лайкам
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Статистика по лайкам")
public class LikeStatisticsDto {

    @Schema(description = "Общее количество лайков", example = "12345")
    private long total;

    @Schema(description = "Поставлено лайков за последние 24 часа", example = "156")
    private long daily;

    @Schema(description = "Поставлено лайков за последнюю неделю", example = "1024")
    private long weekly;
}


package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO со статистикой по стикерсетам
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Статистика по стикерсетам")
public class StickerSetStatisticsDto {

    @Schema(description = "Общее количество стикерсетов (всех состояний)", example = "5432")
    private long total;

    @Schema(description = "Всего публичных стикерсетов", example = "3200")
    private long totalPublic;

    @Schema(description = "Всего приватных стикерсетов", example = "2232")
    private long totalPrivate;

    @Schema(description = "Создано стикерсетов за последние 24 часа (всех)", example = "25")
    private long daily;

    @Schema(description = "Создано публичных стикерсетов за последние 24 часа", example = "15")
    private long dailyPublic;

    @Schema(description = "Создано приватных стикерсетов за последние 24 часа", example = "10")
    private long dailyPrivate;

    @Schema(description = "Создано стикерсетов за последнюю неделю (всех)", example = "180")
    private long weekly;

    @Schema(description = "Создано публичных стикерсетов за последнюю неделю", example = "110")
    private long weeklyPublic;

    @Schema(description = "Создано приватных стикерсетов за последнюю неделю", example = "70")
    private long weeklyPrivate;
}


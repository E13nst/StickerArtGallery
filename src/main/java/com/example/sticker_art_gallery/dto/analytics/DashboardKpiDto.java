package com.example.sticker_art_gallery.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI-карточки для дашборда (агрегаты за выбранный период).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KPI за выбранный период")
public class DashboardKpiDto {

    @Schema(description = "Всего пользователей на конец периода")
    private long totalUsers;
    @Schema(description = "Новых пользователей за период")
    private long newUsers;
    @Schema(description = "Активных пользователей за период (уникальных по событиям)")
    private long activeUsers;

    @Schema(description = "Создано стикерсетов за период")
    private long createdStickerSets;
    @Schema(description = "Лайков за период")
    private long likes;
    @Schema(description = "Дизлайков за период")
    private long dislikes;
    @Schema(description = "Свайпов за период")
    private long swipes;

    @Schema(description = "ART заработано за период")
    private long artEarned;
    @Schema(description = "ART потрачено за период")
    private long artSpent;

    @Schema(description = "Запусков генерации за период")
    private long generationRuns;
    @Schema(description = "Доля успешных генераций, % (0-100)")
    private double generationSuccessRate;

    @Schema(description = "Реферальных конверсий за период")
    private long referralConversions;
    @Schema(description = "Всего реферальных событий за период")
    private long referralEventsTotal;
}

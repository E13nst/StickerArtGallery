package com.example.sticker_art_gallery.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Таймсерии для графиков: по каждому блоку — список точек с bucketStart и value/values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Таймсерии по бакетам для графиков")
public class DashboardTimeseriesDto {

    @Schema(description = "Новые пользователи по бакетам")
    private List<TimeBucketPointDto> newUsers;
    @Schema(description = "Активные пользователи по бакетам")
    private List<TimeBucketPointDto> activeUsers;

    @Schema(description = "Созданные стикерсеты по бакетам")
    private List<TimeBucketPointDto> createdStickerSets;
    @Schema(description = "Лайки по бакетам")
    private List<TimeBucketPointDto> likes;
    @Schema(description = "Дизлайки по бакетам")
    private List<TimeBucketPointDto> dislikes;
    @Schema(description = "Свайпы по бакетам")
    private List<TimeBucketPointDto> swipes;

    @Schema(description = "ART earned по бакетам")
    private List<TimeBucketPointDto> artEarned;
    @Schema(description = "ART spent по бакетам")
    private List<TimeBucketPointDto> artSpent;

    @Schema(description = "Запуски генерации по бакетам")
    private List<TimeBucketPointDto> generationRuns;
    @Schema(description = "Успешные генерации по бакетам")
    private List<TimeBucketPointDto> generationSuccess;

    @Schema(description = "Реферальные события по бакетам")
    private List<TimeBucketPointDto> referralEvents;
}

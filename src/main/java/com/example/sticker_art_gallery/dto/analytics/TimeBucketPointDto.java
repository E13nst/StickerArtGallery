package com.example.sticker_art_gallery.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Одна точка таймсерии: начало бакета и значение/значения по метрикам.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Точка таймсерии по бакету времени")
public class TimeBucketPointDto {

    @Schema(description = "Начало бакета (ISO-8601)", example = "2025-02-01T00:00:00Z")
    private String bucketStart;

    @Schema(description = "Единое значение для простой серии")
    private Long value;

    @Schema(description = "Несколько значений по ключам метрик (например newUsers, activeUsers)")
    private Map<String, Long> values;
}

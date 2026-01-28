package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.model.payment.StarsPackageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * DTO для тарифного пакета Stars
 */
@Getter
@Setter
@Schema(description = "Тарифный пакет для покупки ART за Telegram Stars")
public class StarsPackageDto {

    @Schema(description = "ID пакета", example = "1")
    private Long id;

    @Schema(description = "Уникальный код пакета", example = "STARTER")
    private String code;

    @Schema(description = "Название пакета", example = "Starter Pack")
    private String name;

    @Schema(description = "Описание пакета", example = "100 ART баллов")
    private String description;

    @Schema(description = "Цена в Telegram Stars", example = "50")
    private Integer starsPrice;

    @Schema(description = "Количество ART-баллов", example = "100")
    private Long artAmount;

    @Schema(description = "Порядок сортировки", example = "1")
    private Integer sortOrder;

    @Schema(description = "Включен ли пакет", example = "true")
    private Boolean isEnabled;

    @Schema(description = "Дата создания")
    private OffsetDateTime createdAt;

    public static StarsPackageDto fromEntity(StarsPackageEntity entity) {
        StarsPackageDto dto = new StarsPackageDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStarsPrice(entity.getStarsPrice());
        dto.setArtAmount(entity.getArtAmount());
        dto.setSortOrder(entity.getSortOrder());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

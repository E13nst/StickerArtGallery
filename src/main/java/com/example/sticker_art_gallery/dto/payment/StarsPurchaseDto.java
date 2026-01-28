package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.model.payment.StarsPurchaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * DTO для истории покупок ART за Stars
 */
@Getter
@Setter
@Schema(description = "История покупки ART за Telegram Stars")
public class StarsPurchaseDto {

    @Schema(description = "ID покупки", example = "1")
    private Long id;

    @Schema(description = "Код пакета", example = "STARTER")
    private String packageCode;

    @Schema(description = "Название пакета", example = "Starter Pack")
    private String packageName;

    @Schema(description = "Оплачено Stars", example = "50")
    private Integer starsPaid;

    @Schema(description = "Начислено ART", example = "100")
    private Long artCredited;

    @Schema(description = "Дата покупки")
    private OffsetDateTime createdAt;

    public static StarsPurchaseDto fromEntity(StarsPurchaseEntity entity) {
        StarsPurchaseDto dto = new StarsPurchaseDto();
        dto.setId(entity.getId());
        dto.setPackageCode(entity.getPackageCode());
        if (entity.getStarsPackage() != null) {
            dto.setPackageName(entity.getStarsPackage().getName());
        }
        dto.setStarsPaid(entity.getStarsPaid());
        dto.setArtCredited(entity.getArtCredited());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

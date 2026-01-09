package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Элемент тарифа ART")
public class ArtTariffItem {

    @Schema(description = "Код правила", example = "GENERATE_STICKER")
    private String code;

    @Schema(description = "Количество ART", example = "10")
    private Long amount;

    @Schema(description = "Описание правила", example = "Списание ART за генерацию стикера")
    private String description;

    public ArtTariffItem() {
    }

    public ArtTariffItem(String code, Long amount, String description) {
        this.code = code;
        this.amount = amount;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

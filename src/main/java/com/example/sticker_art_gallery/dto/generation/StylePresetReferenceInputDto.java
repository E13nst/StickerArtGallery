package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Настройки референсных изображений для пресета")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePresetReferenceInputDto {

    @Schema(description = "Разрешена ли загрузка референсов", example = "true")
    private Boolean enabled;

    @Schema(description = "Обязательны ли референсы", example = "false")
    private Boolean required;

    @Schema(description = "Минимум референсов (если required=true, обычно >= 1)", example = "0")
    private Integer minCount;

    @Schema(description = "Максимум референсов, поддерживаемый бэкендом", example = "10")
    private Integer maxCount;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getMinCount() {
        return minCount;
    }

    public void setMinCount(Integer minCount) {
        this.minCount = minCount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }
}

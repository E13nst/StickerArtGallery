package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Запрос на генерацию стикера через Sticker Processor (v2)")
public class GenerateStickerV2Request {

    @Schema(description = "Промпт для генерации", example = "gold dragonfly sticker, transparent background", maxLength = 1000)
    @NotBlank(message = "Промпт не может быть пустым")
    @Size(min = 1, max = 1000, message = "Промпт должен быть от 1 до 1000 символов")
    private String prompt;

    @Schema(description = "Модель генерации", allowableValues = {"flux-schnell", "nanabanana"}, example = "flux-schnell")
    @NotBlank(message = "Поле model обязательно")
    @Pattern(regexp = "flux-schnell|nanabanana", message = "model должен быть flux-schnell или nanabanana")
    private String model;

    @Schema(description = "Размер изображения", example = "512*512", defaultValue = "512*512")
    private String size = "512*512";

    @Schema(description = "Seed для генерации (-1 = случайный)", example = "-1", defaultValue = "-1")
    private Integer seed = -1;

    @JsonProperty("num_images")
    @Schema(description = "Количество изображений (сейчас поддерживается только 1)", example = "1", defaultValue = "1")
    @NotNull(message = "num_images обязателен")
    @Min(value = 1, message = "num_images должен быть >= 1")
    @Max(value = 1, message = "num_images пока поддерживает только значение 1")
    private Integer numImages = 1;

    @Schema(description = "Параметр strength", example = "0.8", defaultValue = "0.8")
    @DecimalMin(value = "0.0", message = "strength должен быть >= 0")
    @DecimalMax(value = "1.0", message = "strength должен быть <= 1")
    private Double strength = 0.8;

    @JsonProperty("remove_background")
    @Schema(description = "Удалить фон", example = "true", defaultValue = "false")
    private Boolean removeBackground = false;

    @JsonProperty("image_id")
    @Schema(description = "ID загруженного изображения для single-image генерации", example = "img_abc123")
    @Pattern(regexp = "^img_[A-Za-z0-9_-]+$", message = "image_id должен иметь формат img_...")
    private String imageId;

    @JsonProperty("image_ids")
    @Schema(description = "Список ID загруженных изображений для multi-image генерации")
    @Size(max = 10, message = "image_ids поддерживает не более 10 элементов")
    private List<
            @Pattern(regexp = "^img_[A-Za-z0-9_-]+$", message = "Каждый image_id должен иметь формат img_...")
            String> imageIds;

    @Schema(description = "ID legacy style preset. Пресет будет применен на этапе prompt processing перед вызовом sticker-processor", example = "1")
    private Long stylePresetId;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Integer getNumImages() {
        return numImages;
    }

    public void setNumImages(Integer numImages) {
        this.numImages = numImages;
    }

    public Double getStrength() {
        return strength;
    }

    public void setStrength(Double strength) {
        this.strength = strength;
    }

    public Boolean getRemoveBackground() {
        return removeBackground;
    }

    public void setRemoveBackground(Boolean removeBackground) {
        this.removeBackground = removeBackground;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public List<String> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds;
    }

    public Long getStylePresetId() {
        return stylePresetId;
    }

    public void setStylePresetId(Long stylePresetId) {
        this.stylePresetId = stylePresetId;
    }

    @AssertTrue(message = "Нужно передать image_id или непустой image_ids")
    public boolean isImageInputValid() {
        if (imageIds != null && !imageIds.isEmpty()) {
            return true;
        }
        return imageId != null && !imageId.isBlank();
    }
}

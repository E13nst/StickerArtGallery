package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на генерацию стикера")
public class GenerateStickerRequest {

    @Schema(description = "Промпт для генерации изображения", example = "A cute cat wearing a hat", required = true, maxLength = 1000)
    @NotBlank(message = "Промпт не может быть пустым")
    @Size(min = 1, max = 1000, message = "Промпт должен быть от 1 до 1000 символов")
    private String prompt;

    @Schema(description = "Seed для генерации (опционально, -1 = случайный)", example = "42", required = false)
    private Integer seed;

    @Schema(description = "Сохранить стикер в стикерсет пользователя после генерации", example = "true", required = false, defaultValue = "false")
    private Boolean saveToStickerSet = false;

    public GenerateStickerRequest() {
    }

    public GenerateStickerRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Boolean getSaveToStickerSet() {
        return saveToStickerSet;
    }

    public void setSaveToStickerSet(Boolean saveToStickerSet) {
        this.saveToStickerSet = saveToStickerSet != null ? saveToStickerSet : false;
    }
}

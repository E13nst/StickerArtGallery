package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO ответа для сохранения изображения в стикерсет.
 * Возвращает file_id стикера, чтобы его можно было сразу использовать в Telegram Bot API (например, sendSticker).
 */
@Schema(description = "Результат сохранения изображения в стикерсет")
public class SaveImageToStickerSetResponseDto {

    @Schema(description = "Имя стикерсета, в который был добавлен стикер",
            example = "user_123456789_by_stixlybot")
    private String stickerSetName;

    @Schema(description = "Индекс стикера в стикерсете (0-based)",
            example = "0")
    private Integer stickerIndex;

    @Schema(description = "Telegram file_id добавленного стикера",
            example = "CAACAgIAAxkBAAE... (file_id)")
    private String stickerFileId;

    @Schema(description = "Название (title) стикерсета",
            example = "Мои стикеры")
    private String title;

    public SaveImageToStickerSetResponseDto() {
    }

    public SaveImageToStickerSetResponseDto(String stickerSetName, Integer stickerIndex, String stickerFileId) {
        this.stickerSetName = stickerSetName;
        this.stickerIndex = stickerIndex;
        this.stickerFileId = stickerFileId;
    }

    public SaveImageToStickerSetResponseDto(String stickerSetName, Integer stickerIndex, String stickerFileId, String title) {
        this.stickerSetName = stickerSetName;
        this.stickerIndex = stickerIndex;
        this.stickerFileId = stickerFileId;
        this.title = title;
    }

    public String getStickerSetName() {
        return stickerSetName;
    }

    public void setStickerSetName(String stickerSetName) {
        this.stickerSetName = stickerSetName;
    }

    public Integer getStickerIndex() {
        return stickerIndex;
    }

    public void setStickerIndex(Integer stickerIndex) {
        this.stickerIndex = stickerIndex;
    }

    public String getStickerFileId() {
        return stickerFileId;
    }

    public void setStickerFileId(String stickerFileId) {
        this.stickerFileId = stickerFileId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}


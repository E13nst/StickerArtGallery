package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат сохранения стикера в набор через Sticker Processor (v2)")
public class SaveToSetV2Response {

    @Schema(description = "Операция с набором", example = "added")
    private String operation;

    @Schema(description = "Имя стикерсета", example = "my_pack_by_bot")
    private String stickerSetName;

    @Schema(description = "Telegram file_id добавленного стикера", example = "CAACAgIAAxkBAAI...")
    private String telegramFileId;

    @Schema(description = "Сервисный статус операции", example = "ok")
    private String status;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getStickerSetName() {
        return stickerSetName;
    }

    public void setStickerSetName(String stickerSetName) {
        this.stickerSetName = stickerSetName;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public void setTelegramFileId(String telegramFileId) {
        this.telegramFileId = telegramFileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

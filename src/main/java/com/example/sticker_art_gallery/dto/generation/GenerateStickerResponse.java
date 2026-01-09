package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на запрос генерации стикера")
public class GenerateStickerResponse {

    @Schema(description = "Уникальный идентификатор задачи генерации", example = "abc123-def456-ghi789")
    private String taskId;

    public GenerateStickerResponse() {
    }

    public GenerateStickerResponse(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}

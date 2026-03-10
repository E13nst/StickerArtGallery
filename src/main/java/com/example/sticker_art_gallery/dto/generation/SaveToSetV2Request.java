package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос сохранения стикера в набор через Sticker Processor (v2)")
public class SaveToSetV2Request {

    @Schema(description = "ID задачи генерации внутри gallery", example = "abc123-def456-ghi789")
    @NotBlank(message = "taskId обязателен")
    private String taskId;

    @Schema(description = "ID пользователя-владельца набора в Telegram", example = "123456789")
    @NotNull(message = "userId обязателен")
    private Long userId;

    @Schema(description = "Short name стикерсета", example = "my_pack_by_bot")
    @NotBlank(message = "name обязателен")
    private String name;

    @Schema(description = "Title стикерсета", example = "My Pack")
    @NotBlank(message = "title обязателен")
    private String title;

    @Schema(description = "Эмодзи для привязки стикера", example = "😀", defaultValue = "😀")
    private String emoji = "😀";

    @JsonProperty("wait_timeout_sec")
    @Schema(description = "Время ожидания готовности генерации в секундах", example = "60", defaultValue = "60")
    @Min(value = 1, message = "wait_timeout_sec должен быть >= 1")
    private Integer waitTimeoutSec = 60;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Integer getWaitTimeoutSec() {
        return waitTimeoutSec;
    }

    public void setWaitTimeoutSec(Integer waitTimeoutSec) {
        this.waitTimeoutSec = waitTimeoutSec;
    }
}

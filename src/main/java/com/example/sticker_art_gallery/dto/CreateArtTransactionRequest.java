package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание ART-транзакции администратором (начисление или списание с отправкой сообщения пользователю).
 */
@Schema(description = "Запрос на создание ART-транзакции (админ)")
public class CreateArtTransactionRequest {

    @NotNull(message = "userId обязателен")
    @Schema(description = "Telegram ID пользователя", example = "123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "amount обязателен и не должен быть нулевым")
    @Schema(description = "Сумма: положительная — начисление (CREDIT), отрицательная — списание (DEBIT). Не может быть 0.", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;

    @Size(max = 4096)
    @Schema(description = "Текст сообщения, которое бот отправит пользователю в личку (опционально)")
    private String message;

    public CreateArtTransactionRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

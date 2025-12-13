package com.example.sticker_art_gallery.dto.transaction;

import com.example.sticker_art_gallery.model.transaction.IntentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO для запроса подготовки транзакции
 */
public class PrepareTransactionRequest {

    @NotNull(message = "ID сущности не может быть null")
    @Positive(message = "ID сущности должен быть положительным числом")
    private Long subjectEntityId;

    @NotNull(message = "Сумма не может быть null")
    @Positive(message = "Сумма должна быть положительным числом")
    private Long amountNano;

    private IntentType intentType = IntentType.DONATION;

    public Long getSubjectEntityId() {
        return subjectEntityId;
    }

    public void setSubjectEntityId(Long subjectEntityId) {
        this.subjectEntityId = subjectEntityId;
    }

    public Long getAmountNano() {
        return amountNano;
    }

    public void setAmountNano(Long amountNano) {
        this.amountNano = amountNano;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }
}


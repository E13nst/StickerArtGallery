package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ответ на создание ART-транзакции: сама транзакция и статус отправки сообщения пользователю.
 */
@Schema(description = "Ответ на создание ART-транзакции")
public class CreateArtTransactionResponse {

    @Schema(description = "Созданная транзакция")
    private ArtTransactionDto transaction;

    @Schema(description = "Сообщение пользователю успешно отправлено ботом")
    private boolean messageSent;

    @Schema(description = "Ошибка отправки сообщения (если messageSent = false и сообщение было указано)")
    private String messageError;

    public CreateArtTransactionResponse() {
    }

    public CreateArtTransactionResponse(ArtTransactionDto transaction, boolean messageSent, String messageError) {
        this.transaction = transaction;
        this.messageSent = messageSent;
        this.messageError = messageError;
    }

    public static CreateArtTransactionResponse success(ArtTransactionDto transaction, boolean messageSent, String messageError) {
        return new CreateArtTransactionResponse(transaction, messageSent, messageError);
    }

    public ArtTransactionDto getTransaction() {
        return transaction;
    }

    public void setTransaction(ArtTransactionDto transaction) {
        this.transaction = transaction;
    }

    public boolean isMessageSent() {
        return messageSent;
    }

    public void setMessageSent(boolean messageSent) {
        this.messageSent = messageSent;
    }

    public String getMessageError() {
        return messageError;
    }

    public void setMessageError(String messageError) {
        this.messageError = messageError;
    }
}

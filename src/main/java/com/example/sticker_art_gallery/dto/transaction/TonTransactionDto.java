package com.example.sticker_art_gallery.dto.transaction;

import com.example.sticker_art_gallery.model.transaction.BlockchainTransactionEntity;
import com.example.sticker_art_gallery.model.transaction.IntentStatus;
import com.example.sticker_art_gallery.model.transaction.IntentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * DTO для TON транзакций в админ-журнале
 */
@Getter
@Setter
@Schema(description = "Информация о TON транзакции для админ-журнала")
public class TonTransactionDto {

    @Schema(description = "ID транзакции", example = "1")
    private Long id;

    @Schema(description = "ID пользователя", example = "123456789")
    private Long userId;

    @Schema(description = "Тип намерения", example = "DONATION")
    private IntentType intentType;

    @Schema(description = "Статус намерения", example = "CONFIRMED")
    private IntentStatus status;

    @Schema(description = "Сумма в nanoTON", example = "1000000000")
    private Long amountNano;

    @Schema(description = "Хеш транзакции", example = "0x1234567890abcdef")
    private String txHash;

    @Schema(description = "Адрес отправителя", example = "EQDummyWallet...")
    private String fromWallet;

    @Schema(description = "Адрес получателя", example = "EQDummyWallet...")
    private String toWallet;

    @Schema(description = "Дата создания транзакции")
    private OffsetDateTime createdAt;

    public static TonTransactionDto fromEntity(BlockchainTransactionEntity entity) {
        TonTransactionDto dto = new TonTransactionDto();
        dto.setId(entity.getId());
        dto.setTxHash(entity.getTxHash());
        dto.setFromWallet(entity.getFromWallet());
        dto.setToWallet(entity.getToWallet());
        dto.setAmountNano(entity.getAmountNano());
        dto.setCreatedAt(entity.getCreatedAt());
        
        // Данные из связанного Intent
        if (entity.getIntent() != null) {
            dto.setUserId(entity.getIntent().getUser() != null ? entity.getIntent().getUser().getId() : null);
            dto.setIntentType(entity.getIntent().getIntentType());
            dto.setStatus(entity.getIntent().getStatus());
        }
        
        return dto;
    }
}

package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Конфигурация для интеграции Stars payments в frontend
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Конфигурация для оплаты через Telegram Stars")
public class StarsConfigDto {

    @Schema(description = "URL внешнего StickerBot API для создания invoice", 
            example = "https://stixly-e13nst.amvera.io")
    private String botApiUrl;

    @Schema(description = "URL webhook для уведомления backend о платеже", 
            example = "https://your-backend.com/api/internal/webhooks/stars-payment")
    private String webhookUrl;

    public static StarsConfigDto of(String botApiUrl, String webhookUrl) {
        return new StarsConfigDto(botApiUrl, webhookUrl);
    }
}

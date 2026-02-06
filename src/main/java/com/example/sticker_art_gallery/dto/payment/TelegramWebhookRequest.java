package com.example.sticker_art_gallery.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запрос webhook от Python сервиса о платеже Telegram Stars
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Webhook запрос о платеже Telegram Stars от Python сервиса")
public class TelegramWebhookRequest {

    @NotBlank(message = "Event обязателен")
    @Pattern(regexp = "telegram_stars_payment_succeeded", message = "Поддерживается только event 'telegram_stars_payment_succeeded'")
    @Schema(description = "Тип события", example = "telegram_stars_payment_succeeded")
    private String event;

    @NotNull(message = "User ID обязателен")
    @JsonProperty("user_id")
    @Schema(description = "ID пользователя Telegram", example = "123456789")
    private Long userId;

    @NotNull(message = "Amount Stars обязателен")
    @JsonProperty("amount_stars")
    @Schema(description = "Количество оплаченных Stars", example = "100")
    private Integer amountStars;

    @NotBlank(message = "Currency обязательна")
    @Pattern(regexp = "XTR", message = "Currency должна быть 'XTR'")
    @Schema(description = "Валюта платежа", example = "XTR")
    private String currency;

    @NotBlank(message = "Telegram charge ID обязателен")
    @JsonProperty("telegram_charge_id")
    @Schema(description = "ID транзакции от Telegram", example = "1234567890")
    private String telegramChargeId;

    @NotBlank(message = "Invoice payload обязателен")
    @JsonProperty("invoice_payload")
    @Schema(description = "Payload invoice (JSON с package_id)", example = "{\"package_id\": 1}")
    private String invoicePayload;

    @NotNull(message = "Timestamp обязателен")
    @Schema(description = "Unix timestamp события", example = "1738500000")
    private Long timestamp;

    /**
     * Парсит package_id из invoice_payload JSON
     * @return ID пакета или null если не удалось распарсить
     */
    public Long getPackageIdFromPayload() {
        if (invoicePayload == null || invoicePayload.trim().isEmpty()) {
            return null;
        }

        try {
            // Парсим JSON payload
            org.json.JSONObject json = new org.json.JSONObject(invoicePayload);
            
            // Извлекаем package_id
            if (json.has("package_id")) {
                Object packageIdObj = json.get("package_id");
                
                // Поддерживаем как число, так и строку
                if (packageIdObj instanceof Number) {
                    return ((Number) packageIdObj).longValue();
                } else if (packageIdObj instanceof String) {
                    return Long.parseLong((String) packageIdObj);
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

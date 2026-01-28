package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Запрос на создание invoice для покупки ART
 */
@Getter
@Setter
@Schema(description = "Запрос на создание invoice для покупки ART за Stars")
public class CreateInvoiceRequest {

    @NotBlank(message = "Код пакета обязателен")
    @Schema(description = "Код тарифного пакета", example = "STARTER", required = true)
    private String packageCode;
}

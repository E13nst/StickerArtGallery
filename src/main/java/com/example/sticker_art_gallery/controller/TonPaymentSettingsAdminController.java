package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.payment.TonPaymentSettingsDto;
import com.example.sticker_art_gallery.service.payment.TonPaymentSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ton-payments/settings")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "TON Payments Settings (Admin)", description = "Настройки TON Pay для покупки ART")
@SecurityRequirement(name = "TelegramInitData")
public class TonPaymentSettingsAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TonPaymentSettingsAdminController.class);

    private final TonPaymentSettingsService settingsService;

    public TonPaymentSettingsAdminController(TonPaymentSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @Operation(summary = "Получить настройки TON Pay")
    public ResponseEntity<TonPaymentSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    @Operation(summary = "Обновить настройки TON Pay")
    public ResponseEntity<TonPaymentSettingsDto> updateSettings(@Valid @RequestBody TonPaymentSettingsDto request) {
        try {
            return ResponseEntity.ok(settingsService.updateSettings(request, getCurrentUserId()));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка обновления TON Pay настроек: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка обновления TON Pay настроек: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }
}

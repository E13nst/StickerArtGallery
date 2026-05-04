package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.payment.CreateTonPaymentRequest;
import com.example.sticker_art_gallery.dto.payment.CreateTonPaymentResponse;
import com.example.sticker_art_gallery.dto.payment.TonPaymentStatusResponse;
import com.example.sticker_art_gallery.service.payment.TonArtPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ton-payments")
@Tag(name = "TON Pay", description = "Покупка ART за TON через TON Pay")
@SecurityRequirement(name = "TelegramInitData")
public class TonArtPaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TonArtPaymentController.class);

    private final TonArtPaymentService tonArtPaymentService;

    public TonArtPaymentController(TonArtPaymentService tonArtPaymentService) {
        this.tonArtPaymentService = tonArtPaymentService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Создать TON Pay платеж для покупки ART-пакета")
    public ResponseEntity<CreateTonPaymentResponse> createPayment(
            @Valid @RequestBody CreateTonPaymentRequest request) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(tonArtPaymentService.createPayment(userId, request));
        } catch (java.util.NoSuchElementException e) {
            LOGGER.warn("TON package not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("TON payment create validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            LOGGER.warn("TON payment create config/state failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            LOGGER.error("TON payment create failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{intentId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить статус TON Pay платежа")
    public ResponseEntity<TonPaymentStatusResponse> getStatus(@PathVariable Long intentId) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(tonArtPaymentService.getStatus(userId, intentId));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error("TON payment status failed: {}", e.getMessage(), e);
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

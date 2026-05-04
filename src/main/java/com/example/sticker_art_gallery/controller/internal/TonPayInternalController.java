package com.example.sticker_art_gallery.controller.internal;

import com.example.sticker_art_gallery.dto.payment.ProcessPaymentResponse;
import com.example.sticker_art_gallery.dto.payment.TonPayWebhookRequest;
import com.example.sticker_art_gallery.security.TonPayWebhookSignatureVerifier;
import com.example.sticker_art_gallery.service.payment.TonArtPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/webhooks")
@PreAuthorize("hasRole('INTERNAL')")
public class TonPayInternalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TonPayInternalController.class);

    private final TonArtPaymentService tonArtPaymentService;
    private final TonPayWebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    public TonPayInternalController(TonArtPaymentService tonArtPaymentService,
                                    TonPayWebhookSignatureVerifier signatureVerifier,
                                    ObjectMapper objectMapper) {
        this.tonArtPaymentService = tonArtPaymentService;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/tonpay")
    public ResponseEntity<?> handleTonPayWebhook(@RequestBody String requestBody,
                                                 HttpServletRequest httpRequest) {
        String signature = httpRequest.getHeader("X-TonPay-Signature");
        TonPayWebhookSignatureVerifier.VerificationResult verification =
                signatureVerifier.verify(requestBody, signature);
        if (!verification.isAllowed()) {
            LOGGER.warn("TON Pay webhook rejected: {}", verification.getReason());
            return ResponseEntity.status(401).body("{\"error\":\"Unauthorized\"}");
        }

        try {
            TonPayWebhookRequest request = objectMapper.readValue(requestBody, TonPayWebhookRequest.class);
            ProcessPaymentResponse response = tonArtPaymentService.processWebhook(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("TON Pay webhook validation failed: {}", e.getMessage());
            return ResponseEntity.ok(ProcessPaymentResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("TON Pay webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProcessPaymentResponse.failure("Внутренняя ошибка обработки TON Pay webhook"));
        }
    }
}

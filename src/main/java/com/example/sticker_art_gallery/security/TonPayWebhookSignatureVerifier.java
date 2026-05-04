package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class TonPayWebhookSignatureVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(TonPayWebhookSignatureVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final AppConfig appConfig;

    public TonPayWebhookSignatureVerifier(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public VerificationResult verify(String rawBody, String signature) {
        String secret = appConfig.getTonpay().getWebhookSecret();
        boolean enforced = appConfig.getTonpay().isWebhookSignatureEnforced();

        if (secret == null || secret.isBlank()) {
            return enforced
                    ? VerificationResult.rejected("TON Pay webhook secret is not configured")
                    : VerificationResult.allowed("TON Pay webhook secret is not configured; check skipped");
        }
        if (signature == null || signature.isBlank()) {
            return enforced
                    ? VerificationResult.rejected("X-TonPay-Signature missing")
                    : VerificationResult.allowed("X-TonPay-Signature missing; check skipped");
        }

        try {
            String expectedHex = computeHmac(secret, rawBody);
            String expected = SIGNATURE_PREFIX + expectedHex;
            String actual = signature.trim();
            boolean valid = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8)
            ) || MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8)
            );
            if (valid) {
                return VerificationResult.allowed("TON Pay signature valid");
            }
            LOGGER.warn("TON Pay signature mismatch: expected={}..., got={}...",
                    expected.substring(0, Math.min(expected.length(), 15)),
                    actual.substring(0, Math.min(actual.length(), 15)));
            return VerificationResult.rejected("TON Pay signature mismatch");
        } catch (Exception e) {
            return VerificationResult.rejected("TON Pay signature check failed: " + e.getMessage());
        }
    }

    private String computeHmac(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static class VerificationResult {
        private final boolean allowed;
        private final String reason;

        private VerificationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        static VerificationResult allowed(String reason) {
            return new VerificationResult(true, reason);
        }

        static VerificationResult rejected(String reason) {
            return new VerificationResult(false, reason);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }
}

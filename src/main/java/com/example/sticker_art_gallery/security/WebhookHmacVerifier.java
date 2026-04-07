package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifier для HMAC-SHA256 подписи webhook-колбэков от StickerBot.
 *
 * Алгоритм подписи (Python-сторона, WebhookNotifier._generate_hmac_signature):
 *   canonical_json = sort_keys + no_spaces (separators=(',', ':'), ensure_ascii=False)
 *   signature = HMAC-SHA256(shared_secret.encode('utf-8'), canonical_json.encode('utf-8')).hexdigest()
 *
 * Заголовок запроса: X-Webhook-Signature: <hex string, 64 символа>
 *
 * Поведение зависит от конфигурации:
 *   - hmac-secret пустой → подпись не проверяется, запрос разрешён
 *   - hmac-secret задан, подпись отсутствует:
 *       hmac-enforced=false → разрешено (warn в лог)
 *       hmac-enforced=true  → запрос отклонён
 *   - hmac-secret задан, подпись присутствует:
 *       совпадает → разрешено
 *       не совпадает → запрос отклонён
 */
@Component
public class WebhookHmacVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookHmacVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AppConfig appConfig;

    public WebhookHmacVerifier(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Проверить подпись входящего webhook-запроса.
     *
     * @param requestBody  сырое тело запроса (строка, как получена из HTTP — canonical JSON от StickerBot)
     * @param signature    значение заголовка X-Webhook-Signature (может быть null)
     * @return результат проверки
     */
    public VerificationResult verify(String requestBody, String signature) {
        String secret = appConfig.getWebhook().getHmacSecret();
        boolean enforced = appConfig.getWebhook().isHmacEnforced();

        // Если секрет не настроен — пропускаем проверку
        if (secret == null || secret.isBlank()) {
            return VerificationResult.noSecret();
        }

        // Секрет настроен, но подпись отсутствует
        if (signature == null || signature.isBlank()) {
            if (enforced) {
                LOGGER.warn("🔒 HMAC enforced: подпись X-Webhook-Signature отсутствует — запрос отклонён");
                return VerificationResult.missingSignature(true);
            } else {
                LOGGER.warn("⚠️ HMAC configured but X-Webhook-Signature absent (hmac-enforced=false) — запрос разрешён");
                return VerificationResult.missingSignature(false);
            }
        }

        // Вычисляем ожидаемую подпись
        String expected;
        try {
            expected = computeHmac(secret, requestBody);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка вычисления HMAC: {}", e.getMessage(), e);
            return VerificationResult.error("HMAC computation failed: " + e.getMessage());
        }

        // Сравнение безопасным способом (timing-safe)
        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8)
        );

        if (valid) {
            return VerificationResult.valid();
        } else {
            LOGGER.warn("🔒 HMAC подпись не совпадает: expected={}..., got={}...",
                    expected.substring(0, Math.min(8, expected.length())),
                    signature.substring(0, Math.min(8, signature.length())));
            return VerificationResult.invalid();
        }
    }

    private String computeHmac(String secret, String body) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hashBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Результат проверки HMAC подписи.
     */
    public static class VerificationResult {
        private final boolean allowed;
        private final boolean signaturePresent;
        private final boolean signatureValid;
        private final String reason;

        private VerificationResult(boolean allowed, boolean signaturePresent,
                                   boolean signatureValid, String reason) {
            this.allowed = allowed;
            this.signaturePresent = signaturePresent;
            this.signatureValid = signatureValid;
            this.reason = reason;
        }

        /** Секрет не настроен — проверка отключена */
        static VerificationResult noSecret() {
            return new VerificationResult(true, false, false, "hmac-secret not configured");
        }

        /** Подпись отсутствует */
        static VerificationResult missingSignature(boolean rejected) {
            return new VerificationResult(!rejected, false, false,
                    rejected ? "X-Webhook-Signature missing (enforced)" : "X-Webhook-Signature missing (not enforced)");
        }

        /** Подпись совпала */
        static VerificationResult valid() {
            return new VerificationResult(true, true, true, "HMAC signature valid");
        }

        /** Подпись не совпала */
        static VerificationResult invalid() {
            return new VerificationResult(false, true, false, "HMAC signature mismatch");
        }

        /** Ошибка вычисления */
        static VerificationResult error(String msg) {
            return new VerificationResult(false, false, false, msg);
        }

        public boolean isAllowed() { return allowed; }
        public boolean isSignaturePresent() { return signaturePresent; }
        public boolean isSignatureValid() { return signatureValid; }
        public String getReason() { return reason; }
    }
}

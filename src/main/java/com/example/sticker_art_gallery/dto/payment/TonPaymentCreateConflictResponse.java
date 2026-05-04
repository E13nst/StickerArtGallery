package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.model.payment.TonPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Структурированный конфликт при создании TON Pay платежа")
public class TonPaymentCreateConflictResponse {

    private TonPaymentCreateConflictCode code;
    private String message;
    private Long intentId;
    private Boolean canResume;
    private TonPaymentStatus status;
    private String packageCode;
    private String expectedSenderAddress;
    private String actualSenderAddress;

    public static TonPaymentCreateConflictResponse intentAlreadyExists(Long intentId,
                                                                       TonPaymentStatus status,
                                                                       String packageCode) {
        TonPaymentCreateConflictResponse response = new TonPaymentCreateConflictResponse();
        response.setCode(TonPaymentCreateConflictCode.INTENT_ALREADY_EXISTS);
        response.setMessage("У пользователя уже есть активное намерение TON-оплаты для этого пакета");
        response.setIntentId(intentId);
        response.setCanResume(Boolean.TRUE);
        response.setStatus(status);
        response.setPackageCode(packageCode);
        return response;
    }

    public static TonPaymentCreateConflictResponse senderAddressMismatch(Long intentId,
                                                                         TonPaymentStatus status,
                                                                         String packageCode,
                                                                         String expectedSenderAddress,
                                                                         String actualSenderAddress) {
        TonPaymentCreateConflictResponse response = new TonPaymentCreateConflictResponse();
        response.setCode(TonPaymentCreateConflictCode.SENDER_ADDRESS_MISMATCH);
        response.setMessage("Адрес отправителя не совпадает с уже созданным активным намерением TON-оплаты");
        response.setIntentId(intentId);
        response.setCanResume(Boolean.FALSE);
        response.setStatus(status);
        response.setPackageCode(packageCode);
        response.setExpectedSenderAddress(expectedSenderAddress);
        response.setActualSenderAddress(actualSenderAddress);
        return response;
    }

    public static TonPaymentCreateConflictResponse tonPaymentsDisabled() {
        TonPaymentCreateConflictResponse response = new TonPaymentCreateConflictResponse();
        response.setCode(TonPaymentCreateConflictCode.TON_PAYMENTS_DISABLED);
        response.setMessage("TON-оплата ART отключена в админке");
        response.setCanResume(Boolean.FALSE);
        return response;
    }

    public static TonPaymentCreateConflictResponse merchantWalletNotConfigured() {
        TonPaymentCreateConflictResponse response = new TonPaymentCreateConflictResponse();
        response.setCode(TonPaymentCreateConflictCode.MERCHANT_WALLET_NOT_CONFIGURED);
        response.setMessage("TON merchant wallet не настроен");
        response.setCanResume(Boolean.FALSE);
        return response;
    }

    public TonPaymentCreateConflictCode getCode() { return code; }
    public void setCode(TonPaymentCreateConflictCode code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public Boolean getCanResume() { return canResume; }
    public void setCanResume(Boolean canResume) { this.canResume = canResume; }
    public TonPaymentStatus getStatus() { return status; }
    public void setStatus(TonPaymentStatus status) { this.status = status; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getExpectedSenderAddress() { return expectedSenderAddress; }
    public void setExpectedSenderAddress(String expectedSenderAddress) { this.expectedSenderAddress = expectedSenderAddress; }
    public String getActualSenderAddress() { return actualSenderAddress; }
    public void setActualSenderAddress(String actualSenderAddress) { this.actualSenderAddress = actualSenderAddress; }
}

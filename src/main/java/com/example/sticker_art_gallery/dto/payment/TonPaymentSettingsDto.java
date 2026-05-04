package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.model.payment.TonPaymentSettingsEntity;
import com.example.sticker_art_gallery.validation.ValidTonAddress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Настройки TON Pay для покупки ART")
public class TonPaymentSettingsDto {

    @ValidTonAddress
    @Schema(description = "Единый кошелёк проекта для приёма TON", example = "EQD...")
    private String merchantWalletAddress;

    @Schema(description = "TON-оплата ART включена глобально", example = "true")
    private Boolean isEnabled;

    @Schema(description = "Есть ли настроенный кошелёк с учётом env fallback", example = "true")
    private Boolean isConfigured;

    @Schema(description = "Источник активного кошелька: db, env или none", example = "db")
    private String source;

    private OffsetDateTime updatedAt;

    public static TonPaymentSettingsDto fromEntity(TonPaymentSettingsEntity entity,
                                                   String effectiveWallet,
                                                   String source) {
        TonPaymentSettingsDto dto = new TonPaymentSettingsDto();
        dto.setMerchantWalletAddress(effectiveWallet);
        dto.setIsEnabled(entity != null ? entity.getIsEnabled() : Boolean.TRUE);
        dto.setIsConfigured(effectiveWallet != null && !effectiveWallet.isBlank());
        dto.setSource(source);
        dto.setUpdatedAt(entity != null ? entity.getUpdatedAt() : null);
        return dto;
    }

    public String getMerchantWalletAddress() { return merchantWalletAddress; }
    public void setMerchantWalletAddress(String merchantWalletAddress) { this.merchantWalletAddress = merchantWalletAddress; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean enabled) { isEnabled = enabled; }
    public Boolean getIsConfigured() { return isConfigured; }
    public void setIsConfigured(Boolean configured) { isConfigured = configured; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

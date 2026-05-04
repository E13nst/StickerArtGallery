package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.payment.TonPaymentSettingsDto;
import com.example.sticker_art_gallery.model.payment.TonPaymentSettingsEntity;
import com.example.sticker_art_gallery.repository.TonPaymentSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TonPaymentSettingsService {

    private final TonPaymentSettingsRepository settingsRepository;
    private final AppConfig appConfig;

    public TonPaymentSettingsService(TonPaymentSettingsRepository settingsRepository, AppConfig appConfig) {
        this.settingsRepository = settingsRepository;
        this.appConfig = appConfig;
    }

    @Transactional(readOnly = true)
    public TonPaymentSettingsDto getSettings() {
        TonPaymentSettingsEntity entity = settingsRepository.findById(TonPaymentSettingsEntity.SINGLETON_ID)
                .orElse(null);
        EffectiveWallet effective = resolveEffectiveWallet(entity);
        return TonPaymentSettingsDto.fromEntity(entity, effective.walletAddress(), effective.source());
    }

    public TonPaymentSettingsDto updateSettings(TonPaymentSettingsDto request, Long adminUserId) {
        TonPaymentSettingsEntity entity = settingsRepository.findById(TonPaymentSettingsEntity.SINGLETON_ID)
                .orElseGet(() -> {
                    TonPaymentSettingsEntity created = new TonPaymentSettingsEntity();
                    created.setId(TonPaymentSettingsEntity.SINGLETON_ID);
                    return created;
                });

        String wallet = request.getMerchantWalletAddress();
        entity.setMerchantWalletAddress(wallet == null || wallet.isBlank() ? null : wallet.trim());
        entity.setIsEnabled(request.getIsEnabled() == null ? Boolean.TRUE : request.getIsEnabled());
        entity.setUpdatedBy(adminUserId);
        entity = settingsRepository.save(entity);

        EffectiveWallet effective = resolveEffectiveWallet(entity);
        return TonPaymentSettingsDto.fromEntity(entity, effective.walletAddress(), effective.source());
    }

    @Transactional(readOnly = true)
    public String resolveMerchantWalletAddress() {
        TonPaymentSettingsEntity entity = settingsRepository.findById(TonPaymentSettingsEntity.SINGLETON_ID)
                .orElse(null);
        if (entity != null && !Boolean.TRUE.equals(entity.getIsEnabled())) {
            throw new IllegalStateException("TON-оплата ART отключена в админке");
        }
        String wallet = resolveEffectiveWallet(entity).walletAddress();
        if (wallet == null || wallet.isBlank()) {
            throw new IllegalStateException("TON merchant wallet не настроен");
        }
        return wallet;
    }

    private EffectiveWallet resolveEffectiveWallet(TonPaymentSettingsEntity entity) {
        if (entity != null && entity.getMerchantWalletAddress() != null && !entity.getMerchantWalletAddress().isBlank()) {
            return new EffectiveWallet(entity.getMerchantWalletAddress(), "db");
        }
        String fallback = appConfig.getTonpay().getMerchantWalletAddress();
        if (fallback != null && !fallback.isBlank()) {
            return new EffectiveWallet(fallback, "env");
        }
        return new EffectiveWallet(null, "none");
    }

    private record EffectiveWallet(String walletAddress, String source) {
    }
}

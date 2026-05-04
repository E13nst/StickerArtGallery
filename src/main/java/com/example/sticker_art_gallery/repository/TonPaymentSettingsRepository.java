package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.TonPaymentSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TonPaymentSettingsRepository extends JpaRepository<TonPaymentSettingsEntity, Short> {
}

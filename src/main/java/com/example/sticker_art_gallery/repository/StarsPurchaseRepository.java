package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.StarsPurchaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StarsPurchaseRepository extends JpaRepository<StarsPurchaseEntity, Long> {

    Optional<StarsPurchaseEntity> findByTelegramPaymentId(String paymentId);

    Optional<StarsPurchaseEntity> findByTelegramChargeId(String chargeId);

    Page<StarsPurchaseEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

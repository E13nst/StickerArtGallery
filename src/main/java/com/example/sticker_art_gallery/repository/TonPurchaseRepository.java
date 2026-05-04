package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.TonPurchaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TonPurchaseRepository extends JpaRepository<TonPurchaseEntity, Long> {

    Optional<TonPurchaseEntity> findByReference(String reference);

    Optional<TonPurchaseEntity> findByTxHash(String txHash);

    boolean existsByTxHash(String txHash);

    Page<TonPurchaseEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

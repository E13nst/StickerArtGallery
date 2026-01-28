package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.InvoiceStatus;
import com.example.sticker_art_gallery.model.payment.StarsInvoiceIntentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StarsInvoiceIntentRepository extends JpaRepository<StarsInvoiceIntentEntity, Long> {

    Optional<StarsInvoiceIntentEntity> findByInvoicePayload(String payload);

    Page<StarsInvoiceIntentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<StarsInvoiceIntentEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, InvoiceStatus status, Pageable pageable);
}

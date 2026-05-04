package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.TonPaymentIntentEntity;
import com.example.sticker_art_gallery.model.payment.TonPaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface TonPaymentIntentRepository extends JpaRepository<TonPaymentIntentEntity, Long> {

    Optional<TonPaymentIntentEntity> findByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from TonPaymentIntentEntity i where i.reference = :reference")
    Optional<TonPaymentIntentEntity> findWithLockByReference(@Param("reference") String reference);

    Page<TonPaymentIntentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<TonPaymentIntentEntity> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            TonPaymentStatus status,
            Pageable pageable
    );

    Optional<TonPaymentIntentEntity> findFirstByUserIdAndPackageCodeAndStatusInOrderByCreatedAtDesc(
            Long userId,
            String packageCode,
            Collection<TonPaymentStatus> statuses
    );
}

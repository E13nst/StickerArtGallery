package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.referral.ReferralEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralEventRepository extends JpaRepository<ReferralEventEntity, Long> {
    
    Optional<ReferralEventEntity> findByExternalId(String externalId);
    
    List<ReferralEventEntity> findByReferral_Id(Long referralId);
    
    List<ReferralEventEntity> findByEventType(String eventType);

    /**
     * Количество реферальных событий за период (для аналитики)
     */
    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}

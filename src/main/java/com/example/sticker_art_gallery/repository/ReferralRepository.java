package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.referral.ReferralEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<ReferralEntity, Long> {
    
    Optional<ReferralEntity> findByReferredUserId(Long referredUserId);
    
    List<ReferralEntity> findByReferrerUserId(Long referrerUserId);
    
    boolean existsByReferredUserId(Long referredUserId);
    
    /**
     * Подсчитать количество начислений рефереру за последние N часов
     */
    @Query("SELECT COUNT(r) FROM ReferralEntity r " +
           "WHERE r.referrerUserId = :referrerUserId " +
           "AND r.referrerFirstGenerationAwardedAt >= :since")
    long countByReferrerUserIdAndReferrerFirstGenerationAwardedAtAfter(
            @Param("referrerUserId") Long referrerUserId,
            @Param("since") OffsetDateTime since);
}

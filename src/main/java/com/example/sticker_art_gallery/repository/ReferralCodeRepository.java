package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.referral.ReferralCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralCodeRepository extends JpaRepository<ReferralCodeEntity, Long> {
    
    Optional<ReferralCodeEntity> findByCode(String code);
    
    boolean existsByCode(String code);
}

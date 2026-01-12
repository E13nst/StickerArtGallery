package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtRuleRepository extends JpaRepository<ArtRuleEntity, Long> {

    Optional<ArtRuleEntity> findByCode(String code);

    boolean existsByCode(String code);
}

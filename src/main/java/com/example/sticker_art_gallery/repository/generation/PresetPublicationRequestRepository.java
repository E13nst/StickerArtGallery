package com.example.sticker_art_gallery.repository.generation;

import com.example.sticker_art_gallery.model.generation.PresetPublicationRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresetPublicationRequestRepository extends JpaRepository<PresetPublicationRequestEntity, Long> {

    Optional<PresetPublicationRequestEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}

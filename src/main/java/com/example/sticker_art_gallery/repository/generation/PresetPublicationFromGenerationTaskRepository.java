package com.example.sticker_art_gallery.repository.generation;

import com.example.sticker_art_gallery.model.generation.PresetPublicationFromGenerationTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresetPublicationFromGenerationTaskRepository
        extends JpaRepository<PresetPublicationFromGenerationTaskEntity, Long> {

    Optional<PresetPublicationFromGenerationTaskEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByGenerationTaskIdAndStatus(String generationTaskId, String status);
}

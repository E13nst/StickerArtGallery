package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTaskEntity, String> {

    Optional<GenerationTaskEntity> findByTaskId(String taskId);

    Page<GenerationTaskEntity> findByUserProfile_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT g FROM GenerationTaskEntity g WHERE g.expiresAt < :now AND g.status IN ('COMPLETED', 'FAILED')")
    java.util.List<GenerationTaskEntity> findByExpiresAtBefore(OffsetDateTime now);

    @Query("SELECT g FROM GenerationTaskEntity g WHERE g.status IN ('PENDING', 'GENERATING', 'REMOVING_BACKGROUND') AND g.expiresAt > :now")
    java.util.List<GenerationTaskEntity> findActiveTasks(OffsetDateTime now);
}

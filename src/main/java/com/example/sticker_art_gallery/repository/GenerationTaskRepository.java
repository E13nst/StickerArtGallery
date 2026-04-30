package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTaskEntity, String> {

    Optional<GenerationTaskEntity> findByTaskId(String taskId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GenerationTaskEntity g JOIN FETCH g.userProfile WHERE g.taskId = :taskId")
    Optional<GenerationTaskEntity> findByTaskIdForUpdate(@Param("taskId") String taskId);

    Page<GenerationTaskEntity> findByUserProfile_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query(
            value = """
                    SELECT *
                    FROM generation_tasks g
                    WHERE g.user_id = :userId
                      AND COALESCE(g.metadata::jsonb ->> 'flow', 'legacy') = 'generation-v2'
                    ORDER BY g.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM generation_tasks g
                    WHERE g.user_id = :userId
                      AND COALESCE(g.metadata::jsonb ->> 'flow', 'legacy') = 'generation-v2'
                    """,
            nativeQuery = true
    )
    Page<GenerationTaskEntity> findV2ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    SELECT *
                    FROM generation_tasks g
                    WHERE COALESCE(g.metadata::jsonb ->> 'flow', 'legacy') = 'generation-v2'
                      AND (:userId IS NULL OR g.user_id = :userId)
                      AND (:status IS NULL OR g.status = :status)
                      AND (:taskId IS NULL OR g.task_id = :taskId)
                    ORDER BY g.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM generation_tasks g
                    WHERE COALESCE(g.metadata::jsonb ->> 'flow', 'legacy') = 'generation-v2'
                      AND (:userId IS NULL OR g.user_id = :userId)
                      AND (:status IS NULL OR g.status = :status)
                      AND (:taskId IS NULL OR g.task_id = :taskId)
                    """,
            nativeQuery = true
    )
    Page<GenerationTaskEntity> findV2ForAdmin(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("taskId") String taskId,
            Pageable pageable
    );

    @Query("SELECT g FROM GenerationTaskEntity g WHERE g.expiresAt < :now AND g.status IN ('COMPLETED', 'FAILED')")
    java.util.List<GenerationTaskEntity> findByExpiresAtBefore(OffsetDateTime now);

    @Query("SELECT g FROM GenerationTaskEntity g WHERE g.status IN ('PENDING', 'GENERATING', 'REMOVING_BACKGROUND') AND g.expiresAt > :now")
    java.util.List<GenerationTaskEntity> findActiveTasks(OffsetDateTime now);

    @Query(
            value = """
                    SELECT *
                    FROM generation_tasks g
                    WHERE g.user_id = :userId
                      AND g.status = 'COMPLETED'
                      AND g.cached_image_id IS NOT NULL
                      AND (g.metadata::jsonb ->> 'stylePresetId') ~ '^[0-9]+$'
                      AND ((g.metadata::jsonb ->> 'stylePresetId')::bigint = :stylePresetId)
                    ORDER BY g.completed_at DESC NULLS LAST, g.created_at DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<GenerationTaskEntity> findLatestCompletedForUserAndPreset(
            @Param("userId") Long userId,
            @Param("stylePresetId") Long stylePresetId
    );
}

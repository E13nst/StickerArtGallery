package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.GenerationAuditSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationAuditSessionRepository extends JpaRepository<GenerationAuditSessionEntity, Long> {

    Optional<GenerationAuditSessionEntity> findByTaskId(String taskId);

    @Query("SELECT s FROM GenerationAuditSessionEntity s WHERE " +
            "(:userId is null OR s.userId = :userId) AND " +
            "(:finalStatus is null OR s.finalStatus = :finalStatus) AND " +
            "(COALESCE(:dateFrom, s.startedAt) <= s.startedAt) AND " +
            "(s.startedAt <= COALESCE(:dateTo, s.startedAt)) AND " +
            "(:errorOnly is null OR :errorOnly = false OR s.errorCode is not null) AND " +
            "(:taskId is null OR s.taskId = :taskId) " +
            "ORDER BY s.startedAt DESC")
    Page<GenerationAuditSessionEntity> findWithFilters(
            @Param("userId") Long userId,
            @Param("finalStatus") String finalStatus,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            @Param("errorOnly") Boolean errorOnly,
            @Param("taskId") String taskId,
            Pageable pageable);

    List<GenerationAuditSessionEntity> findByExpiresAtBefore(OffsetDateTime now);
}

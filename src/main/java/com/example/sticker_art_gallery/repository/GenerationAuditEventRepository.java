package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.GenerationAuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenerationAuditEventRepository extends JpaRepository<GenerationAuditEventEntity, Long> {

    @Query("SELECT e FROM GenerationAuditEventEntity e WHERE e.taskId = :taskId ORDER BY e.createdAt ASC")
    List<GenerationAuditEventEntity> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") String taskId);

    @Query("SELECT e FROM GenerationAuditEventEntity e WHERE e.session.id = :sessionId ORDER BY e.createdAt ASC")
    Page<GenerationAuditEventEntity> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId, Pageable pageable);
}

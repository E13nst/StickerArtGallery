package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.messaging.MessageAuditSessionEntity;
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
public interface MessageAuditSessionRepository extends JpaRepository<MessageAuditSessionEntity, Long> {

    Optional<MessageAuditSessionEntity> findByMessageId(String messageId);

    @Query("SELECT s FROM MessageAuditSessionEntity s WHERE " +
            "(:userId is null OR s.userId = :userId) AND " +
            "(:finalStatus is null OR s.finalStatus = :finalStatus) AND " +
            "(COALESCE(:dateFrom, s.startedAt) <= s.startedAt) AND " +
            "(s.startedAt <= COALESCE(:dateTo, s.startedAt)) AND " +
            "(:errorOnly is null OR :errorOnly = false OR s.errorCode is not null) AND " +
            "(:messageId is null OR s.messageId = :messageId) " +
            "ORDER BY s.startedAt DESC")
    Page<MessageAuditSessionEntity> findWithFilters(
            @Param("userId") Long userId,
            @Param("finalStatus") String finalStatus,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            @Param("errorOnly") Boolean errorOnly,
            @Param("messageId") String messageId,
            Pageable pageable);

    List<MessageAuditSessionEntity> findByExpiresAtBefore(OffsetDateTime now);
}

package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.messaging.MessageAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageAuditEventRepository extends JpaRepository<MessageAuditEventEntity, Long> {

    List<MessageAuditEventEntity> findByMessageIdOrderByCreatedAtAsc(String messageId);
}

package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.messaging.MessageAuditEventDto;
import com.example.sticker_art_gallery.dto.messaging.MessageAuditSessionDto;
import com.example.sticker_art_gallery.model.messaging.MessageAuditSessionEntity;
import com.example.sticker_art_gallery.repository.MessageAuditEventRepository;
import com.example.sticker_art_gallery.repository.MessageAuditSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageAuditQueryService {

    private final MessageAuditSessionRepository sessionRepository;
    private final MessageAuditEventRepository eventRepository;

    public MessageAuditQueryService(
            MessageAuditSessionRepository sessionRepository,
            MessageAuditEventRepository eventRepository) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageAuditSessionDto> findWithFilters(
            Long userId,
            String finalStatus,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            Boolean errorOnly,
            String messageId,
            Pageable pageable) {
        Page<MessageAuditSessionEntity> page = sessionRepository.findWithFilters(
                userId, finalStatus, dateFrom, dateTo, errorOnly, messageId, pageable);
        List<MessageAuditSessionDto> content = page.getContent().stream()
                .map(MessageAuditSessionDto::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public MessageAuditSessionDto getByMessageId(String messageId) {
        return sessionRepository.findByMessageId(messageId)
                .map(MessageAuditSessionDto::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MessageAuditEventDto> getEventsByMessageId(String messageId) {
        return eventRepository.findByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .map(MessageAuditEventDto::fromEntity)
                .collect(Collectors.toList());
    }
}

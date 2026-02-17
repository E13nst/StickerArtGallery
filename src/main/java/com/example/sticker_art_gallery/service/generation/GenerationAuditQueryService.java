package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.generation.GenerationAuditEventDto;
import com.example.sticker_art_gallery.dto.generation.GenerationAuditSessionDto;
import com.example.sticker_art_gallery.model.generation.GenerationAuditSessionEntity;
import com.example.sticker_art_gallery.repository.GenerationAuditEventRepository;
import com.example.sticker_art_gallery.repository.GenerationAuditSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenerationAuditQueryService {

    private final GenerationAuditSessionRepository sessionRepository;
    private final GenerationAuditEventRepository eventRepository;

    public GenerationAuditQueryService(
            GenerationAuditSessionRepository sessionRepository,
            GenerationAuditEventRepository eventRepository) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<GenerationAuditSessionDto> findWithFilters(
            Long userId,
            String finalStatus,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            Boolean errorOnly,
            String taskId,
            Pageable pageable) {
        Page<GenerationAuditSessionEntity> page = sessionRepository.findWithFilters(
                userId, finalStatus, dateFrom, dateTo, errorOnly, taskId, pageable);
        List<GenerationAuditSessionDto> content = page.getContent().stream()
                .map(GenerationAuditSessionDto::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public GenerationAuditSessionDto getByTaskId(String taskId) {
        return sessionRepository.findByTaskId(taskId)
                .map(GenerationAuditSessionDto::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GenerationAuditEventDto> getEventsByTaskId(String taskId) {
        return eventRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(GenerationAuditEventDto::fromEntity)
                .collect(Collectors.toList());
    }
}

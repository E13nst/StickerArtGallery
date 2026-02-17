package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationAuditEventEntity;
import com.example.sticker_art_gallery.model.generation.GenerationAuditSessionEntity;
import com.example.sticker_art_gallery.model.generation.GenerationAuditStage;
import com.example.sticker_art_gallery.repository.GenerationAuditEventRepository;
import com.example.sticker_art_gallery.repository.GenerationAuditSessionRepository;
import com.example.sticker_art_gallery.repository.UserProfileRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Epic("Генерация стикеров")
@Feature("Audit-лог генерации")
@DisplayName("GenerationAuditService: запись сессий и событий")
@Tag("integration")
class GenerationAuditServiceTest {

    private static final Long USER_ID = TestDataBuilder.TEST_USER_ID;
    private static final String TASK_ID = "test-task-audit-001";

    @Autowired
    private GenerationAuditService auditService;
    @Autowired
    private GenerationAuditSessionRepository sessionRepository;
    @Autowired
    private GenerationAuditEventRepository eventRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        if (!userProfileRepository.existsByUserId(USER_ID)) {
            userProfileRepository.save(TestDataBuilder.createTestUserProfile(USER_ID));
        }
    }

    @Test
    @Story("Создание сессии и событий")
    @DisplayName("startSession создает сессию и событие REQUEST_ACCEPTED")
    void startSession_createsSessionAndEvent() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(90);
        auditService.startSession(TASK_ID, USER_ID, "raw prompt", Map.of("seed", 42), expiresAt);

        GenerationAuditSessionEntity session = sessionRepository.findByTaskId(TASK_ID).orElse(null);
        assertThat(session).isNotNull();
        assertThat(session.getUserId()).isEqualTo(USER_ID);
        assertThat(session.getRawPrompt()).isEqualTo("raw prompt");
        assertThat(session.getRequestParams()).contains("42");

        List<GenerationAuditEventEntity> events = eventRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStage()).isEqualTo(GenerationAuditStage.REQUEST_ACCEPTED);
    }

    @Test
    @Story("Завершение успехом")
    @DisplayName("finishSuccess обновляет сессию и добавляет событие COMPLETED")
    void finishSuccess_updatesSessionAndAddsEvent() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(90);
        auditService.startSession(TASK_ID, USER_ID, "prompt", null, expiresAt);
        auditService.finishSuccess(TASK_ID, Map.of("imageUrl", "https://example.com/img.png"));

        GenerationAuditSessionEntity session = sessionRepository.findByTaskId(TASK_ID).orElse(null);
        assertThat(session).isNotNull();
        assertThat(session.getFinalStatus()).isEqualTo("COMPLETED");
        assertThat(session.getCompletedAt()).isNotNull();

        List<GenerationAuditEventEntity> events = eventRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID);
        assertThat(events).hasSize(2);
        assertThat(events.get(1).getStage()).isEqualTo(GenerationAuditStage.COMPLETED);
    }

    @Test
    @Story("Завершение с ошибкой")
    @DisplayName("finishFailure записывает errorCode и errorMessage")
    void finishFailure_recordsError() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(90);
        auditService.startSession(TASK_ID, USER_ID, "prompt", null, expiresAt);
        auditService.finishFailure(TASK_ID, GenerationAuditService.ERROR_WAVESPEED_TIMEOUT, "Timed out", null);

        GenerationAuditSessionEntity session = sessionRepository.findByTaskId(TASK_ID).orElse(null);
        assertThat(session).isNotNull();
        assertThat(session.getFinalStatus()).isEqualTo("FAILED");
        assertThat(session.getErrorCode()).isEqualTo(GenerationAuditService.ERROR_WAVESPEED_TIMEOUT);
        assertThat(session.getErrorMessage()).isEqualTo("Timed out");
    }
}

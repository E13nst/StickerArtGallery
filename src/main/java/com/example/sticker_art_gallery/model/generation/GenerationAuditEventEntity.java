package com.example.sticker_art_gallery.model.generation;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "generation_audit_events")
public class GenerationAuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GenerationAuditSessionEntity session;

    @Column(name = "task_id", nullable = false, length = 255)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 80)
    private GenerationAuditStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 50)
    private GenerationAuditEventStatus eventStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GenerationAuditSessionEntity getSession() {
        return session;
    }

    public void setSession(GenerationAuditSessionEntity session) {
        this.session = session;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public GenerationAuditStage getStage() {
        return stage;
    }

    public void setStage(GenerationAuditStage stage) {
        this.stage = stage;
    }

    public GenerationAuditEventStatus getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(GenerationAuditEventStatus eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

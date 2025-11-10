package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.profile.ArtTransactionDirection;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;

import java.time.OffsetDateTime;

public class ArtTransactionDto {

    private Long id;
    private Long userId;
    private String ruleCode;
    private ArtTransactionDirection direction;
    private Long delta;
    private Long balanceAfter;
    private String metadata;
    private String externalId;
    private Long performedBy;
    private OffsetDateTime createdAt;

    public static ArtTransactionDto fromEntity(ArtTransactionEntity entity) {
        ArtTransactionDto dto = new ArtTransactionDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setRuleCode(entity.getRuleCode());
        dto.setDirection(entity.getDirection());
        dto.setDelta(entity.getDelta());
        dto.setBalanceAfter(entity.getBalanceAfter());
        dto.setMetadata(entity.getMetadata());
        dto.setExternalId(entity.getExternalId());
        dto.setPerformedBy(entity.getPerformedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public ArtTransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(ArtTransactionDirection direction) {
        this.direction = direction;
    }

    public Long getDelta() {
        return delta;
    }

    public void setDelta(Long delta) {
        this.delta = delta;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


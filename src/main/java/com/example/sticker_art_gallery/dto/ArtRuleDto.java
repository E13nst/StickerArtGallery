package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class ArtRuleDto {

    private Long id;

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotNull
    private ArtTransactionDirection direction;

    @NotNull
    @Min(0)
    private Long amount;

    private Boolean isEnabled;

    private String description;

    private String metadataSchema;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ArtRuleDto fromEntity(ArtRuleEntity entity) {
        ArtRuleDto dto = new ArtRuleDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setDirection(entity.getDirection());
        dto.setAmount(entity.getAmount());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setDescription(entity.getDescription());
        dto.setMetadataSchema(entity.getMetadataSchema());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public ArtRuleEntity toEntity() {
        ArtRuleEntity entity = new ArtRuleEntity();
        entity.setId(this.id);
        entity.setCode(this.code);
        entity.setDirection(this.direction);
        entity.setAmount(this.amount);
        entity.setIsEnabled(this.isEnabled != null ? this.isEnabled : Boolean.TRUE);
        entity.setDescription(this.description);
        entity.setMetadataSchema(this.metadataSchema);
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);
        return entity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ArtTransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(ArtTransactionDirection direction) {
        this.direction = direction;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadataSchema() {
        return metadataSchema;
    }

    public void setMetadataSchema(String metadataSchema) {
        this.metadataSchema = metadataSchema;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


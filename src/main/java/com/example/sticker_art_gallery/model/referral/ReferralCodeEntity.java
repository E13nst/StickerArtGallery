package com.example.sticker_art_gallery.model.referral;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "referral_codes")
public class ReferralCodeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

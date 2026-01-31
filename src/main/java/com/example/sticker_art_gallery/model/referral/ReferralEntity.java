package com.example.sticker_art_gallery.model.referral;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "referrals")
public class ReferralEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referrer_user_id", nullable = false)
    private Long referrerUserId;

    @Column(name = "referred_user_id", nullable = false, unique = true)
    private Long referredUserId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "start_param", length = 64)
    private String startParam;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "invitee_bonus_awarded_at")
    private OffsetDateTime inviteeBonusAwardedAt;

    @Column(name = "referrer_first_generation_awarded_at")
    private OffsetDateTime referrerFirstGenerationAwardedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReferrerUserId() {
        return referrerUserId;
    }

    public void setReferrerUserId(Long referrerUserId) {
        this.referrerUserId = referrerUserId;
    }

    public Long getReferredUserId() {
        return referredUserId;
    }

    public void setReferredUserId(Long referredUserId) {
        this.referredUserId = referredUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartParam() {
        return startParam;
    }

    public void setStartParam(String startParam) {
        this.startParam = startParam;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getInviteeBonusAwardedAt() {
        return inviteeBonusAwardedAt;
    }

    public void setInviteeBonusAwardedAt(OffsetDateTime inviteeBonusAwardedAt) {
        this.inviteeBonusAwardedAt = inviteeBonusAwardedAt;
    }

    public OffsetDateTime getReferrerFirstGenerationAwardedAt() {
        return referrerFirstGenerationAwardedAt;
    }

    public void setReferrerFirstGenerationAwardedAt(OffsetDateTime referrerFirstGenerationAwardedAt) {
        this.referrerFirstGenerationAwardedAt = referrerFirstGenerationAwardedAt;
    }
}

package com.example.sticker_art_gallery.model.referral;

import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "referral_events")
public class ReferralEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id", nullable = false)
    private ReferralEntity referral;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "art_transaction_id")
    private ArtTransactionEntity artTransaction;

    @Column(name = "external_id", nullable = false, unique = true, length = 128)
    private String externalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

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

    public ReferralEntity getReferral() {
        return referral;
    }

    public void setReferral(ReferralEntity referral) {
        this.referral = referral;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public ArtTransactionEntity getArtTransaction() {
        return artTransaction;
    }

    public void setArtTransaction(ArtTransactionEntity artTransaction) {
        this.artTransaction = artTransaction;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

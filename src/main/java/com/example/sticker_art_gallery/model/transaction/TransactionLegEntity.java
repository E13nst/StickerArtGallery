package com.example.sticker_art_gallery.model.transaction;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity для частей транзакций
 */
@Entity
@Table(name = "transaction_legs")
public class TransactionLegEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intent_id", nullable = false)
    private TransactionIntentEntity intent;

    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type", nullable = false)
    private LegType legType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_entity_id")
    private PlatformEntityEntity toEntity;

    @Column(name = "to_wallet_address", nullable = false)
    private String toWalletAddress;

    @Column(name = "amount_nano", nullable = false)
    private Long amountNano;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionIntentEntity getIntent() {
        return intent;
    }

    public void setIntent(TransactionIntentEntity intent) {
        this.intent = intent;
    }

    public LegType getLegType() {
        return legType;
    }

    public void setLegType(LegType legType) {
        this.legType = legType;
    }

    public PlatformEntityEntity getToEntity() {
        return toEntity;
    }

    public void setToEntity(PlatformEntityEntity toEntity) {
        this.toEntity = toEntity;
    }

    public String getToWalletAddress() {
        return toWalletAddress;
    }

    public void setToWalletAddress(String toWalletAddress) {
        this.toWalletAddress = toWalletAddress;
    }

    public Long getAmountNano() {
        return amountNano;
    }

    public void setAmountNano(Long amountNano) {
        this.amountNano = amountNano;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionLegEntity that = (TransactionLegEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


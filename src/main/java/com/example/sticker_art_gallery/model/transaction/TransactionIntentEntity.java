package com.example.sticker_art_gallery.model.transaction;

import com.example.sticker_art_gallery.model.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity для намерений транзакций (источник истины)
 */
@Entity
@Table(name = "transaction_intents")
public class TransactionIntentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent_type", nullable = false)
    private IntentType intentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_entity_id")
    private PlatformEntityEntity subjectEntity;

    @Column(name = "amount_nano", nullable = false)
    private Long amountNano;

    @Column(name = "currency", nullable = false)
    private String currency = "TON";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IntentStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "intent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionLegEntity> legs = new ArrayList<>();

    @OneToMany(mappedBy = "intent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlockchainTransactionEntity> blockchainTransactions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.currency == null) {
            this.currency = "TON";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public PlatformEntityEntity getSubjectEntity() {
        return subjectEntity;
    }

    public void setSubjectEntity(PlatformEntityEntity subjectEntity) {
        this.subjectEntity = subjectEntity;
    }

    public Long getAmountNano() {
        return amountNano;
    }

    public void setAmountNano(Long amountNano) {
        this.amountNano = amountNano;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public IntentStatus getStatus() {
        return status;
    }

    public void setStatus(IntentStatus status) {
        this.status = status;
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

    public List<TransactionLegEntity> getLegs() {
        return legs;
    }

    public void setLegs(List<TransactionLegEntity> legs) {
        this.legs = legs;
    }

    public List<BlockchainTransactionEntity> getBlockchainTransactions() {
        return blockchainTransactions;
    }

    public void setBlockchainTransactions(List<BlockchainTransactionEntity> blockchainTransactions) {
        this.blockchainTransactions = blockchainTransactions;
    }
}


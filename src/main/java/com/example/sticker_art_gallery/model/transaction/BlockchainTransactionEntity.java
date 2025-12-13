package com.example.sticker_art_gallery.model.transaction;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity для транзакций в блокчейне TON
 */
@Entity
@Table(name = "blockchain_transactions")
public class BlockchainTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intent_id", nullable = false)
    private TransactionIntentEntity intent;

    @Column(name = "tx_hash", nullable = false, unique = true)
    private String txHash;

    @Column(name = "from_wallet", nullable = false)
    private String fromWallet;

    @Column(name = "to_wallet", nullable = false)
    private String toWallet;

    @Column(name = "amount_nano", nullable = false)
    private Long amountNano;

    @Column(name = "currency", nullable = false)
    private String currency = "TON";

    @Column(name = "block_time")
    private OffsetDateTime blockTime;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

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

    public TransactionIntentEntity getIntent() {
        return intent;
    }

    public void setIntent(TransactionIntentEntity intent) {
        this.intent = intent;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getFromWallet() {
        return fromWallet;
    }

    public void setFromWallet(String fromWallet) {
        this.fromWallet = fromWallet;
    }

    public String getToWallet() {
        return toWallet;
    }

    public void setToWallet(String toWallet) {
        this.toWallet = toWallet;
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

    public OffsetDateTime getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(OffsetDateTime blockTime) {
        this.blockTime = blockTime;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


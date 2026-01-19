package com.example.sticker_art_gallery.model.profile;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_profiles")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "userProfiles")
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId; // FK на users (Telegram ID)

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 16, nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "art_balance", nullable = false)
    private Long artBalance = 0L;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", length = 16, nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.NONE;

    @Column(name = "subscription_expires_at")
    private OffsetDateTime subscriptionExpiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum UserRole {
        USER,
        ADMIN
    }

    public enum SubscriptionStatus {
        NONE,
        ACTIVE,
        EXPIRED,
        CANCELLED
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getArtBalance() {
        return artBalance;
    }

    public void setArtBalance(Long artBalance) {
        this.artBalance = artBalance;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
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

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public OffsetDateTime getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public void setSubscriptionExpiresAt(OffsetDateTime subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    /**
     * Проверяет, есть ли у пользователя активная подписка.
     * Подписка считается активной, если:
     * - статус = ACTIVE
     * - дата окончания не null и не истекла
     */
    public boolean hasActiveSubscription() {
        return subscriptionStatus == SubscriptionStatus.ACTIVE
            && subscriptionExpiresAt != null
            && subscriptionExpiresAt.isAfter(OffsetDateTime.now());
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}



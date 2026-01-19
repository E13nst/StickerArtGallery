package com.example.sticker_art_gallery.model.swipe;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.OffsetDateTime;

/**
 * Entity для конфигурации системы отслеживания свайпов.
 * Singleton таблица - только одна запись с id=1.
 */
@Entity
@Table(name = "swipe_config")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "swipeConfig")
public class SwipeConfigEntity {

    @Id
    private Long id = 1L; // Singleton - всегда id=1

    @Column(name = "swipes_per_reward", nullable = false)
    private Integer swipesPerReward = 50;

    @Column(name = "reward_amount", nullable = false)
    private Long rewardAmount = 50L;

    @Column(name = "daily_limit_regular", nullable = false)
    private Integer dailyLimitRegular = 50;

    @Column(name = "daily_limit_premium", nullable = false)
    private Integer dailyLimitPremium = 100;

    @Column(name = "reward_amount_premium", nullable = false)
    private Long rewardAmountPremium = 100L;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_type", nullable = false, length = 16)
    private ResetType resetType = ResetType.MIDNIGHT;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum ResetType {
        MIDNIGHT,      // Сброс в полночь по серверному времени
        ROLLING_24H    // Через 24 часа после первого свайпа
    }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSwipesPerReward() {
        return swipesPerReward;
    }

    public void setSwipesPerReward(Integer swipesPerReward) {
        this.swipesPerReward = swipesPerReward;
    }

    public Long getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(Long rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public Integer getDailyLimitRegular() {
        return dailyLimitRegular;
    }

    public void setDailyLimitRegular(Integer dailyLimitRegular) {
        this.dailyLimitRegular = dailyLimitRegular;
    }

    public Integer getDailyLimitPremium() {
        return dailyLimitPremium;
    }

    public void setDailyLimitPremium(Integer dailyLimitPremium) {
        this.dailyLimitPremium = dailyLimitPremium;
    }

    public Long getRewardAmountPremium() {
        return rewardAmountPremium;
    }

    public void setRewardAmountPremium(Long rewardAmountPremium) {
        this.rewardAmountPremium = rewardAmountPremium;
    }

    public ResetType getResetType() {
        return resetType;
    }

    public void setResetType(ResetType resetType) {
        this.resetType = resetType;
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

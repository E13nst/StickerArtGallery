package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * DTO для статистики свайпов пользователя
 */
@Schema(description = "Статистика свайпов пользователя")
public class SwipeStatsDto {

    @Schema(description = "Количество свайпов за сегодня", example = "25")
    private int dailySwipes;

    @Schema(description = "Дневной лимит свайпов", example = "50")
    private int dailyLimit;

    @Schema(description = "Оставшееся количество свайпов", example = "25")
    private int remainingSwipes;

    @Schema(description = "Есть ли у пользователя активная подписка", example = "false")
    private boolean hasSubscription;

    @Schema(description = "Дата окончания подписки", example = "2026-02-17T00:00:00Z")
    private OffsetDateTime subscriptionExpiresAt;

    @Schema(description = "Количество свайпов для получения награды", example = "50")
    private int swipesPerReward;

    @Schema(description = "Сколько свайпов осталось до следующей награды", example = "25")
    private int swipesUntilReward;

    // Конструкторы
    public SwipeStatsDto() {
    }

    public SwipeStatsDto(int dailySwipes, int dailyLimit, int remainingSwipes,
                        boolean hasSubscription, OffsetDateTime subscriptionExpiresAt,
                        int swipesPerReward, int swipesUntilReward) {
        this.dailySwipes = dailySwipes;
        this.dailyLimit = dailyLimit;
        this.remainingSwipes = remainingSwipes;
        this.hasSubscription = hasSubscription;
        this.subscriptionExpiresAt = subscriptionExpiresAt;
        this.swipesPerReward = swipesPerReward;
        this.swipesUntilReward = swipesUntilReward;
    }

    // Геттеры и сеттеры
    public int getDailySwipes() {
        return dailySwipes;
    }

    public void setDailySwipes(int dailySwipes) {
        this.dailySwipes = dailySwipes;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getRemainingSwipes() {
        return remainingSwipes;
    }

    public void setRemainingSwipes(int remainingSwipes) {
        this.remainingSwipes = remainingSwipes;
    }

    public boolean isHasSubscription() {
        return hasSubscription;
    }

    public void setHasSubscription(boolean hasSubscription) {
        this.hasSubscription = hasSubscription;
    }

    public OffsetDateTime getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public void setSubscriptionExpiresAt(OffsetDateTime subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public int getSwipesPerReward() {
        return swipesPerReward;
    }

    public void setSwipesPerReward(int swipesPerReward) {
        this.swipesPerReward = swipesPerReward;
    }

    public int getSwipesUntilReward() {
        return swipesUntilReward;
    }

    public void setSwipesUntilReward(int swipesUntilReward) {
        this.swipesUntilReward = swipesUntilReward;
    }

    @Override
    public String toString() {
        return "SwipeStatsDto{" +
                "dailySwipes=" + dailySwipes +
                ", dailyLimit=" + dailyLimit +
                ", remainingSwipes=" + remainingSwipes +
                ", hasSubscription=" + hasSubscription +
                ", subscriptionExpiresAt=" + subscriptionExpiresAt +
                ", swipesPerReward=" + swipesPerReward +
                ", swipesUntilReward=" + swipesUntilReward +
                '}';
    }
}

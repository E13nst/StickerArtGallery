package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для обновления профиля пользователя (только для администраторов)
 */
@Schema(description = "Запрос на обновление профиля пользователя")
public class UpdateUserProfileRequest {
    
    @Schema(description = "Роль пользователя", example = "USER", allowableValues = {"USER", "ADMIN"})
    private UserProfileEntity.UserRole role;
    
    @Schema(description = "Флаг блокировки пользователя", example = "false")
    private Boolean isBlocked;
    
    @Schema(description = "Статус подписки", example = "ACTIVE", 
            allowableValues = {"NONE", "ACTIVE", "EXPIRED", "CANCELLED"})
    private UserProfileEntity.SubscriptionStatus subscriptionStatus;
    
    // Конструкторы
    public UpdateUserProfileRequest() {
    }
    
    public UpdateUserProfileRequest(UserProfileEntity.UserRole role,
                                   Boolean isBlocked,
                                   UserProfileEntity.SubscriptionStatus subscriptionStatus) {
        this.role = role;
        this.isBlocked = isBlocked;
        this.subscriptionStatus = subscriptionStatus;
    }
    
    // Геттеры и сеттеры
    public UserProfileEntity.UserRole getRole() {
        return role;
    }
    
    public void setRole(UserProfileEntity.UserRole role) {
        this.role = role;
    }
    
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public UserProfileEntity.SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }
    
    public void setSubscriptionStatus(UserProfileEntity.SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
    
    @Override
    public String toString() {
        return "UpdateUserProfileRequest{" +
                "role=" + role +
                ", isBlocked=" + isBlocked +
                ", subscriptionStatus=" + subscriptionStatus +
                '}';
    }
}

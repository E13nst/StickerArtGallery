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
    
    @Schema(description = "Баланс ART-кредитов", example = "100")
    private Long artBalance;
    
    @Schema(description = "Флаг блокировки пользователя", example = "false")
    private Boolean isBlocked;
    
    @Schema(description = "Статус подписки", example = "ACTIVE", 
            allowableValues = {"NONE", "ACTIVE", "EXPIRED", "CANCELLED"})
    private UserProfileEntity.SubscriptionStatus subscriptionStatus;
    
    // Конструкторы
    public UpdateUserProfileRequest() {
    }
    
    public UpdateUserProfileRequest(UserProfileEntity.UserRole role, 
                                   Long artBalance, 
                                   Boolean isBlocked,
                                   UserProfileEntity.SubscriptionStatus subscriptionStatus) {
        this.role = role;
        this.artBalance = artBalance;
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
                ", artBalance=" + artBalance +
                ", isBlocked=" + isBlocked +
                ", subscriptionStatus=" + subscriptionStatus +
                '}';
    }
}

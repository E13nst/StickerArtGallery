package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

/**
 * DTO для профиля пользователя
 */
public class UserProfileDto {
    
    @NotNull(message = "ID пользователя не может быть null")
    @Positive(message = "ID пользователя должен быть положительным числом")
    private Long userId;
    
    @Pattern(regexp = "^(USER|ADMIN)$", message = "Роль должна быть USER или ADMIN")
    private String role;
    
    @NotNull(message = "Баланс арт-кредитов не может быть null")
    @Min(value = 0, message = "Баланс арт-кредитов не может быть отрицательным")
    private Long artBalance;
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Конструкторы
    public UserProfileDto() {}
    
    public UserProfileDto(Long userId, String role, Long artBalance, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.userId = userId;
        this.role = role;
        this.artBalance = artBalance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Создает DTO из Entity
     */
    public static UserProfileDto fromEntity(UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserProfileDto(
                entity.getUserId(),
                entity.getRole().name(),
                entity.getArtBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
    
    /**
     * Создает Entity из DTO
     */
    public UserProfileEntity toEntity() {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setUserId(this.userId);
        if (this.role != null) {
            entity.setRole(UserProfileEntity.UserRole.valueOf(this.role));
        }
        entity.setArtBalance(this.artBalance);
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);
        return entity;
    }
    
    // Геттеры и сеттеры
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public Long getArtBalance() { return artBalance; }
    public void setArtBalance(Long artBalance) { this.artBalance = artBalance; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "UserProfileDto{" +
                "userId=" + userId +
                ", role='" + role + '\'' +
                ", artBalance=" + artBalance +
                '}';
    }
}

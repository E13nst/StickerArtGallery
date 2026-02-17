package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

/**
 * DTO для профиля пользователя
 */
public class UserProfileDto {
    
    private Long id; // ID профиля (автоинкремент)
    
    @NotNull(message = "ID пользователя не может быть null")
    @Positive(message = "ID пользователя должен быть положительным числом")
    private Long userId; // Telegram ID
    
    @Pattern(regexp = "^(USER|ADMIN)$", message = "Роль должна быть USER или ADMIN")
    private String role;
    
    @NotNull(message = "Баланс арт-кредитов не может быть null")
    @Min(value = 0, message = "Баланс арт-кредитов не может быть отрицательным")
    private Long artBalance;

    private Boolean isBlocked;
    
    private UserDto user; // Информация о пользователе из Telegram
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Счетчики стикерсетов (только для админского списка)
    private Long ownedStickerSetsCount;
    private Long verifiedStickerSetsCount;
    
    // Конструкторы
    public UserProfileDto() {}
    
    public UserProfileDto(Long id, Long userId, String role, Long artBalance, Boolean isBlocked, UserDto user, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.artBalance = artBalance;
        this.isBlocked = isBlocked;
        this.user = user;
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
                entity.getId(),
                entity.getUserId(),
                entity.getRole().name(),
                entity.getArtBalance(),
                entity.getIsBlocked(),
                null, // user будет установлен отдельно в контроллере
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
    
    /**
     * Создает Entity из DTO
     */
    public UserProfileEntity toEntity() {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setId(this.id);
        entity.setUserId(this.userId);
        if (this.role != null) {
            entity.setRole(UserProfileEntity.UserRole.valueOf(this.role));
        }
        entity.setArtBalance(this.artBalance);
        entity.setIsBlocked(this.isBlocked);
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);
        return entity;
    }
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public Long getArtBalance() { return artBalance; }
    public void setArtBalance(Long artBalance) { this.artBalance = artBalance; }

    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }
    
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getOwnedStickerSetsCount() { return ownedStickerSetsCount; }
    public void setOwnedStickerSetsCount(Long ownedStickerSetsCount) { this.ownedStickerSetsCount = ownedStickerSetsCount; }
    
    public Long getVerifiedStickerSetsCount() { return verifiedStickerSetsCount; }
    public void setVerifiedStickerSetsCount(Long verifiedStickerSetsCount) { this.verifiedStickerSetsCount = verifiedStickerSetsCount; }
    
    @Override
    public String toString() {
        return "UserProfileDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", role='" + role + '\'' +
                ", artBalance=" + artBalance +
                ", isBlocked=" + isBlocked +
                '}';
    }
}

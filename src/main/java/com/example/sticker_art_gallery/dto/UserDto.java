package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.user.UserEntity;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

/**
 * DTO для данных пользователя из Telegram
 */
public class UserDto {
    
    @NotNull(message = "ID пользователя не может быть null")
    @Positive(message = "ID пользователя должен быть положительным числом")
    private Long id;
    
    @Size(max = 255, message = "Имя не может быть длиннее 255 символов")
    private String firstName;
    
    @Size(max = 255, message = "Фамилия не может быть длиннее 255 символов")
    private String lastName;
    
    @Size(max = 255, message = "Username не может быть длиннее 255 символов")
    private String username;
    
    @Size(max = 10, message = "Код языка не может быть длиннее 10 символов")
    private String languageCode;
    
    private Boolean isPremium;
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Конструкторы
    public UserDto() {}
    
    public UserDto(Long id, String firstName, String lastName, String username, 
                  String languageCode, Boolean isPremium,
                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.languageCode = languageCode;
        this.isPremium = isPremium;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Создает DTO из Entity
     */
    public static UserDto fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUsername(),
                entity.getLanguageCode(),
                entity.getIsPremium(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    
    public Boolean getIsPremium() { return isPremium; }
    public void setIsPremium(Boolean isPremium) { this.isPremium = isPremium; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", isPremium=" + isPremium +
                '}';
    }
}


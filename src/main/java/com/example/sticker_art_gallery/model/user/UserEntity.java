package com.example.sticker_art_gallery.model.user;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity пользователя системы
 */
@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    private Long id; // Теперь id = telegram_id
    
    @Column(name = "username", length = 255)
    private String username;
    
    @Column(name = "first_name", length = 255)
    private String firstName;
    
    @Column(name = "last_name", length = 255)
    private String lastName;
    
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;
    
    @Column(name = "art_balance", nullable = false)
    private Long artBalance = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 16, nullable = false)
    private UserRole role = UserRole.USER;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    // Конструкторы
    public UserEntity() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
    
    public UserEntity(Long id, String username, String firstName, String lastName, String avatarUrl) {
        this();
        this.id = id; // id теперь = telegram_id
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
    }
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    // telegramId теперь доступен через getId()
    public Long getTelegramId() { return id; }
    public void setTelegramId(Long telegramId) { this.id = telegramId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public Long getArtBalance() { return artBalance; }
    public void setArtBalance(Long artBalance) { this.artBalance = artBalance; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
    
    /**
     * Роли пользователей
     */
    public enum UserRole {
        USER,
        ADMIN
    }
    
    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                ", artBalance=" + artBalance +
                '}';
    }
}

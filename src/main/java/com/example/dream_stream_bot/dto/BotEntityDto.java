package com.example.dream_stream_bot.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BotEntityDto {
    
    private Long id;
    private String name;
    private String username;
    private String token;
    private String prompt;
    private String webhookUrl;
    private String type;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    private String triggers;
    private Integer memWindow;
    private String miniapp;
    
    // Конструкторы
    public BotEntityDto() {}
    
    public BotEntityDto(Long id, String name, String username, String token, String prompt, 
                       String webhookUrl, String type, Boolean isActive, LocalDateTime createdAt, 
                       LocalDateTime updatedAt, String description, String triggers, 
                       Integer memWindow, String miniapp) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.token = token;
        this.prompt = prompt;
        this.webhookUrl = webhookUrl;
        this.type = type;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.description = description;
        this.triggers = triggers;
        this.memWindow = memWindow;
        this.miniapp = miniapp;
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTriggers() {
        return triggers;
    }
    
    public void setTriggers(String triggers) {
        this.triggers = triggers;
    }
    
    public Integer getMemWindow() {
        return memWindow;
    }
    
    public void setMemWindow(Integer memWindow) {
        this.memWindow = memWindow;
    }
    
    public String getMiniapp() {
        return miniapp;
    }
    
    public void setMiniapp(String miniapp) {
        this.miniapp = miniapp;
    }
    
    // Конструктор для создания DTO из Entity
    public static BotEntityDto fromEntity(com.example.dream_stream_bot.model.telegram.BotEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new BotEntityDto(
            entity.getId(),
            entity.getName(),
            entity.getUsername(),
            entity.getToken(),
            entity.getPrompt(),
            entity.getWebhookUrl(),
            entity.getType(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDescription(),
            entity.getTriggers(),
            entity.getMemWindow(),
            entity.getMiniapp()
        );
    }
    
    // Методы для работы со списками
    public List<String> getBotAliasesList() {
        if (name == null) return List.of();
        return List.of(name.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
    
    public List<String> getBotTriggersList() {
        if (triggers == null) return List.of();
        return List.of(triggers.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
    
    @Override
    public String toString() {
        return "BotEntityDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", type='" + type + '\'' +
                ", isActive=" + isActive +
                ", miniapp='" + miniapp + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

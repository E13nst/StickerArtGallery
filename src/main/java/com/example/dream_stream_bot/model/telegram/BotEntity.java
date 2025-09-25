package com.example.dream_stream_bot.model.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bot")
@Data
public class BotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name; // отображаемое имя

    @Column(nullable = false, length = 64)
    private String username; // username в Telegram

    @Column(nullable = false, length = 128)
    private String token;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "webhook_url", length = 256)
    private String webhookUrl;

    @Column(nullable = false, length = 32)
    private String type; // тип бота

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(length = 256)
    private String description;

    @Column(length = 256)
    private String triggers;

    @Column(name = "mem_window")
    private Integer memWindow = 100;

    @Column(name = "miniapp", length = 512)
    private String miniapp; // ссылка на миниприложение Telegram

    public java.util.List<String> getBotAliasesList() {
        if (name == null) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(name.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
    public java.util.List<String> getBotTriggersList() {
        if (triggers == null) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(triggers.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
    public String getBotName() {
        return name;
    }
} 
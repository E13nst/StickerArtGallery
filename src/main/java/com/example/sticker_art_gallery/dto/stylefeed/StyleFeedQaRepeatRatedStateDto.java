package com.example.sticker_art_gallery.dto.stylefeed;

/**
 * Состояние QA-режима «повторно показывать оценённые карточки style feed».
 */
public class StyleFeedQaRepeatRatedStateDto {

    /** Итоговый telegram user id (0 = выключено): БД, иначе env. */
    private long effectiveTelegramUserId;
    /** Значение в БД или null, если переопределения нет — тогда действует только env. */
    private String databaseValueOrNull;
    /** Значение из application.yaml / env без учёта БД (>0 или 0 если не задано). */
    private long environmentFallbackUserId;
    private boolean databaseOverridePresent;

    public long getEffectiveTelegramUserId() {
        return effectiveTelegramUserId;
    }

    public void setEffectiveTelegramUserId(long effectiveTelegramUserId) {
        this.effectiveTelegramUserId = effectiveTelegramUserId;
    }

    public String getDatabaseValueOrNull() {
        return databaseValueOrNull;
    }

    public void setDatabaseValueOrNull(String databaseValueOrNull) {
        this.databaseValueOrNull = databaseValueOrNull;
    }

    public long getEnvironmentFallbackUserId() {
        return environmentFallbackUserId;
    }

    public void setEnvironmentFallbackUserId(long environmentFallbackUserId) {
        this.environmentFallbackUserId = environmentFallbackUserId;
    }

    public boolean isDatabaseOverridePresent() {
        return databaseOverridePresent;
    }

    public void setDatabaseOverridePresent(boolean databaseOverridePresent) {
        this.databaseOverridePresent = databaseOverridePresent;
    }
}

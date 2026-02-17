package com.example.sticker_art_gallery.repository.projection;

import java.time.Instant;

/**
 * Projection для получения профиля пользователя со счетчиками стикерсетов
 * Используется в админ-панели для отображения списка пользователей
 * 
 * Примечание: использует Instant для timestamp колонок, так как Spring Data
 * при native query маппит TIMESTAMPTZ в Instant, а не OffsetDateTime
 */
public interface UserProfileWithStickerCountsProjection {
    
    Long getId();
    
    Long getUserId();
    
    String getRole();
    
    Long getArtBalance();
    
    Boolean getIsBlocked();
    
    String getSubscriptionStatus();
    
    Instant getCreatedAt();
    
    Instant getUpdatedAt();
    
    /**
     * Количество стикерсетов, где пользователь является владельцем (user_id)
     */
    Long getOwnedStickerSetsCount();
    
    /**
     * Количество верифицированных стикерсетов (user_id + is_verified=true)
     */
    Long getVerifiedStickerSetsCount();
}

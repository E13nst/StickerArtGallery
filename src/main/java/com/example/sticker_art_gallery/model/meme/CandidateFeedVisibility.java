package com.example.sticker_art_gallery.model.meme;

/**
 * Видимость мем-кандидата в пользовательской ленте оценки.
 * Логика приоритета:
 *   если admin_visibility_override IS NOT NULL → он главный;
 *   иначе → автоправило (AUTO_HIDDEN при >= 7 дизлайков из > 10 голосов).
 */
public enum CandidateFeedVisibility {
    VISIBLE,
    AUTO_HIDDEN,
    ADMIN_HIDDEN,
    ADMIN_FORCED_VISIBLE
}

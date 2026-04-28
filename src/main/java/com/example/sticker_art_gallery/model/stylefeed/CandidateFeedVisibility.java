package com.example.sticker_art_gallery.model.stylefeed;

/**
 * Видимость записи ленты style feed в пользовательской оценке.
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

package com.example.sticker_art_gallery.dto.deck;

/**
 * Действие по карточке колоды API {@code POST /api/deck/action}.
 */
public enum DeckCardAction {
    LIKE,
    DISLIKE,
    DISMISS,
    CLAIM,
    OPEN,
    PRIMARY_CTA
}

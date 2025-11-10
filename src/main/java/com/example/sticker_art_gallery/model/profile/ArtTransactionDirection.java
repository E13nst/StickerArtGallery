package com.example.sticker_art_gallery.model.profile;

/**
 * Направление движения ART-баллов.
 */
public enum ArtTransactionDirection {
    CREDIT,
    DEBIT;

    public long apply(long amount) {
        long abs = Math.abs(amount);
        return this == CREDIT ? abs : -abs;
    }
}


package com.example.sticker_art_gallery.dto.deck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /api/deck/action}
 */
public class DeckCardActionRequest {

    @NotBlank
    private String cardInstanceId;

    @NotNull
    private DeckCardAction action;

    public String getCardInstanceId() {
        return cardInstanceId;
    }

    public void setCardInstanceId(String cardInstanceId) {
        this.cardInstanceId = cardInstanceId;
    }

    public DeckCardAction getAction() {
        return action;
    }

    public void setAction(DeckCardAction action) {
        this.action = action;
    }
}

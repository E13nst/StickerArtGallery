package com.example.sticker_art_gallery.dto.deck;

import com.example.sticker_art_gallery.dto.SwipeStatsDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckCardsResponse {

    private List<DeckCardDto> cards;
    private SwipeStatsDto swipeStats;
    private DeckProgressDto deckProgress;

    public List<DeckCardDto> getCards() {
        return cards;
    }

    public void setCards(List<DeckCardDto> cards) {
        this.cards = cards;
    }

    public SwipeStatsDto getSwipeStats() {
        return swipeStats;
    }

    public void setSwipeStats(SwipeStatsDto swipeStats) {
        this.swipeStats = swipeStats;
    }

    public DeckProgressDto getDeckProgress() {
        return deckProgress;
    }

    public void setDeckProgress(DeckProgressDto deckProgress) {
        this.deckProgress = deckProgress;
    }
}

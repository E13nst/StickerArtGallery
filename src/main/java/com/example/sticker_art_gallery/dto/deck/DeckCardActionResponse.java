package com.example.sticker_art_gallery.dto.deck;

import com.example.sticker_art_gallery.dto.SwipeStatsDto;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckCardActionResponse {

    private boolean success;
    private Long artDelta;
    private Long balanceAfter;
    private DeckProgressDto deckProgress;
    private SwipeStatsDto swipeStats;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getArtDelta() {
        return artDelta;
    }

    public void setArtDelta(Long artDelta) {
        this.artDelta = artDelta;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public DeckProgressDto getDeckProgress() {
        return deckProgress;
    }

    public void setDeckProgress(DeckProgressDto deckProgress) {
        this.deckProgress = deckProgress;
    }

    public SwipeStatsDto getSwipeStats() {
        return swipeStats;
    }

    public void setSwipeStats(SwipeStatsDto swipeStats) {
        this.swipeStats = swipeStats;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

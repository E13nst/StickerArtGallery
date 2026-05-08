package com.example.sticker_art_gallery.service.deck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeckConstantsTest {

    @Test
    @DisplayName("Размер пачки API и пороги награды за прогон колоды (20 / 40) зафиксированы")
    void shouldMatchDeckBatchAndRewardThresholds() {
        assertThat(DeckConstants.DEFAULT_DECK_BATCH_SIZE).isEqualTo(20);
        assertThat(DeckConstants.FREE_SWIPES_PER_DECK_REWARD).isEqualTo(20);
        assertThat(DeckConstants.PREMIUM_SWIPES_PER_DECK_REWARD).isEqualTo(40);
    }
}

package com.example.sticker_art_gallery.dto.deck;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckProgressDto {

    /** Стилевых свайпов с последней награды за прогон колоды. */
    private int styleSwipesInCurrentRun;
    /** Порог для текущего пользователя ({@code 20} free / {@code 40} premium). */
    private int swipesRequiredForDeckReward;
    /** Осталось стилевых свайпов до награды за прогон (0, если событие на очереди следующего свайпа = награда). */
    private int swipesRemainingUntilDeckReward;
    private boolean premium;
    private long deckCompletionsTotal;

    public int getStyleSwipesInCurrentRun() {
        return styleSwipesInCurrentRun;
    }

    public void setStyleSwipesInCurrentRun(int styleSwipesInCurrentRun) {
        this.styleSwipesInCurrentRun = styleSwipesInCurrentRun;
    }

    public int getSwipesRequiredForDeckReward() {
        return swipesRequiredForDeckReward;
    }

    public void setSwipesRequiredForDeckReward(int swipesRequiredForDeckReward) {
        this.swipesRequiredForDeckReward = swipesRequiredForDeckReward;
    }

    public int getSwipesRemainingUntilDeckReward() {
        return swipesRemainingUntilDeckReward;
    }

    public void setSwipesRemainingUntilDeckReward(int swipesRemainingUntilDeckReward) {
        this.swipesRemainingUntilDeckReward = swipesRemainingUntilDeckReward;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public long getDeckCompletionsTotal() {
        return deckCompletionsTotal;
    }

    public void setDeckCompletionsTotal(long deckCompletionsTotal) {
        this.deckCompletionsTotal = deckCompletionsTotal;
    }
}

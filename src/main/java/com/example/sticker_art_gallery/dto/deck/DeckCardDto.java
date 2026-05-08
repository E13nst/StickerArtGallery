package com.example.sticker_art_gallery.dto.deck;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckCardDto {

    private String cardInstanceId;
    private DeckCardType type;
    private int priority;
    private boolean consumesSwipeLimit;
    private boolean grantsSwipeReward;
    private List<DeckCardAction> actions;
    private Map<String, Object> payload;

    public String getCardInstanceId() {
        return cardInstanceId;
    }

    public void setCardInstanceId(String cardInstanceId) {
        this.cardInstanceId = cardInstanceId;
    }

    public DeckCardType getType() {
        return type;
    }

    public void setType(DeckCardType type) {
        this.type = type;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isConsumesSwipeLimit() {
        return consumesSwipeLimit;
    }

    public void setConsumesSwipeLimit(boolean consumesSwipeLimit) {
        this.consumesSwipeLimit = consumesSwipeLimit;
    }

    public boolean isGrantsSwipeReward() {
        return grantsSwipeReward;
    }

    public void setGrantsSwipeReward(boolean grantsSwipeReward) {
        this.grantsSwipeReward = grantsSwipeReward;
    }

    public List<DeckCardAction> getActions() {
        return actions;
    }

    public void setActions(List<DeckCardAction> actions) {
        this.actions = actions;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}

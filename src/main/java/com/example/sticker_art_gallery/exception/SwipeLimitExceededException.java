package com.example.sticker_art_gallery.exception;

import com.example.sticker_art_gallery.model.swipe.SwipeConfigEntity;

/**
 * Исключение, выбрасываемое при достижении дневного лимита свайпов
 */
public class SwipeLimitExceededException extends RuntimeException {

    private final int dailyLimit;
    private final int currentSwipes;
    private final SwipeConfigEntity.ResetType resetType;

    public SwipeLimitExceededException(int dailyLimit, int currentSwipes, 
                                      SwipeConfigEntity.ResetType resetType, 
                                      String message) {
        super(message);
        this.dailyLimit = dailyLimit;
        this.currentSwipes = currentSwipes;
        this.resetType = resetType;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public int getCurrentSwipes() {
        return currentSwipes;
    }

    public SwipeConfigEntity.ResetType getResetType() {
        return resetType;
    }

    public String getResetTypeDescription() {
        return resetType == SwipeConfigEntity.ResetType.MIDNIGHT 
            ? "счетчик сбросится в полночь" 
            : "счетчик сбросится через 24 часа после первого свайпа";
    }
}

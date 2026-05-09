package com.example.sticker_art_gallery.dto.stylefeed;

/**
 * Результат сброса голосов style feed для одного пользователя (для QA / админки).
 */
public class StyleFeedVotesResetResultDto {

    private long userId;
    private int deletedStyleFeedSwipeRows;
    private int deletedLikeRows;
    private int deletedDislikeRows;
    private int recomputedStyleFeedItemRows;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getDeletedStyleFeedSwipeRows() {
        return deletedStyleFeedSwipeRows;
    }

    public void setDeletedStyleFeedSwipeRows(int deletedStyleFeedSwipeRows) {
        this.deletedStyleFeedSwipeRows = deletedStyleFeedSwipeRows;
    }

    public int getDeletedLikeRows() {
        return deletedLikeRows;
    }

    public void setDeletedLikeRows(int deletedLikeRows) {
        this.deletedLikeRows = deletedLikeRows;
    }

    public int getDeletedDislikeRows() {
        return deletedDislikeRows;
    }

    public void setDeletedDislikeRows(int deletedDislikeRows) {
        this.deletedDislikeRows = deletedDislikeRows;
    }

    public int getRecomputedStyleFeedItemRows() {
        return recomputedStyleFeedItemRows;
    }

    public void setRecomputedStyleFeedItemRows(int recomputedStyleFeedItemRows) {
        this.recomputedStyleFeedItemRows = recomputedStyleFeedItemRows;
    }
}

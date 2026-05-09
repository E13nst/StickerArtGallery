package com.example.sticker_art_gallery.dto.stylefeed;

/**
 * Агрегированная статистика голосов ленты style feed (таблицы style_feed_item_*).
 */
public class StyleFeedVotesStatsDto {

    /** Всего строк в style_feed_item_likes. */
    private long styleFeedLikesTotal;
    /** Всего строк в style_feed_item_dislikes. */
    private long styleFeedDislikesTotal;
    /** Уникальных user_id, которые голосовали (лайк или дизлайк). */
    private long distinctVoters;

    public long getStyleFeedLikesTotal() {
        return styleFeedLikesTotal;
    }

    public void setStyleFeedLikesTotal(long styleFeedLikesTotal) {
        this.styleFeedLikesTotal = styleFeedLikesTotal;
    }

    public long getStyleFeedDislikesTotal() {
        return styleFeedDislikesTotal;
    }

    public void setStyleFeedDislikesTotal(long styleFeedDislikesTotal) {
        this.styleFeedDislikesTotal = styleFeedDislikesTotal;
    }

    public long getDistinctVoters() {
        return distinctVoters;
    }

    public void setDistinctVoters(long distinctVoters) {
        this.distinctVoters = distinctVoters;
    }
}

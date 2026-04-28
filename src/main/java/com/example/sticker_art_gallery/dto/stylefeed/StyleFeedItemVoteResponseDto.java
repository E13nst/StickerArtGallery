package com.example.sticker_art_gallery.dto.stylefeed;

/**
 * Ответ на лайк/дизлайк записи style feed.
 */
public class StyleFeedItemVoteResponseDto {

    private Long id;
    private Long userId;
    private Long styleFeedItemId;
    private boolean liked;
    private boolean disliked;
    private int totalLikes;
    private int totalDislikes;
    private boolean swipe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStyleFeedItemId() { return styleFeedItemId; }
    public void setStyleFeedItemId(Long styleFeedItemId) { this.styleFeedItemId = styleFeedItemId; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isDisliked() { return disliked; }
    public void setDisliked(boolean disliked) { this.disliked = disliked; }

    public int getTotalLikes() { return totalLikes; }
    public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }

    public int getTotalDislikes() { return totalDislikes; }
    public void setTotalDislikes(int totalDislikes) { this.totalDislikes = totalDislikes; }

    public boolean isSwipe() { return swipe; }
    public void setSwipe(boolean swipe) { this.swipe = swipe; }
}

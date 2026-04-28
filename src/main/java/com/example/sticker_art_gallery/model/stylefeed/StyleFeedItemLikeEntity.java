package com.example.sticker_art_gallery.model.stylefeed;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Лайк записи ленты style feed.
 */
@Entity
@Table(name = "style_feed_item_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_style_feed_item_like",
                columnNames = {"user_id", "style_feed_item_id"}))
public class StyleFeedItemLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_feed_item_id", nullable = false)
    private StyleFeedItemEntity styleFeedItem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public StyleFeedItemEntity getStyleFeedItem() { return styleFeedItem; }
    public void setStyleFeedItem(StyleFeedItemEntity styleFeedItem) { this.styleFeedItem = styleFeedItem; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyleFeedItemLikeEntity that = (StyleFeedItemLikeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StyleFeedItemLikeEntity{id=" + id + ", userId=" + userId + '}';
    }
}

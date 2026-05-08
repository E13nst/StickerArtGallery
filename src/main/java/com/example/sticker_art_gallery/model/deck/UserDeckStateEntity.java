package com.example.sticker_art_gallery.model.deck;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_deck_state")
public class UserDeckStateEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "style_swipes_in_run", nullable = false)
    private int styleSwipesInRun;

    @Column(name = "deck_completions_total", nullable = false)
    private long deckCompletionsTotal;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getStyleSwipesInRun() {
        return styleSwipesInRun;
    }

    public void setStyleSwipesInRun(int styleSwipesInRun) {
        this.styleSwipesInRun = styleSwipesInRun;
    }

    public long getDeckCompletionsTotal() {
        return deckCompletionsTotal;
    }

    public void setDeckCompletionsTotal(long deckCompletionsTotal) {
        this.deckCompletionsTotal = deckCompletionsTotal;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDeckStateEntity that = (UserDeckStateEntity) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}

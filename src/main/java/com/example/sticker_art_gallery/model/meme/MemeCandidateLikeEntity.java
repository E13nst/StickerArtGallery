package com.example.sticker_art_gallery.model.meme;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Лайк мем-кандидата.
 * Аналог {@link com.example.sticker_art_gallery.model.Like} для стикерсетов.
 * unique constraint: (user_id, meme_candidate_id).
 */
@Entity
@Table(name = "meme_candidate_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_meme_candidate_like",
                columnNames = {"user_id", "meme_candidate_id"}))
public class MemeCandidateLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meme_candidate_id", nullable = false)
    private MemeCandidateEntity memeCandidate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public MemeCandidateEntity getMemeCandidate() { return memeCandidate; }
    public void setMemeCandidate(MemeCandidateEntity memeCandidate) { this.memeCandidate = memeCandidate; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemeCandidateLikeEntity that = (MemeCandidateLikeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "MemeCandidateLikeEntity{id=" + id + ", userId=" + userId + '}';
    }
}

package com.example.sticker_art_gallery.model.swipe;

import com.example.sticker_art_gallery.model.Dislike;
import com.example.sticker_art_gallery.model.Like;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entity для хранения истории свайпов пользователей.
 * Свайпом считается лайк или дизлайк, установленный с флагом свайп.
 */
@Entity
@Table(name = "user_swipes")
public class UserSwipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 16)
    private ActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "like_id")
    private Like like;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dislike_id")
    private Dislike dislike;

    @Column(name = "swipe_date", nullable = false)
    private LocalDate swipeDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum ActionType {
        LIKE,
        DISLIKE
    }

    // Конструкторы
    public UserSwipeEntity() {
    }

    public UserSwipeEntity(Long userId, ActionType actionType, Like like, Dislike dislike, LocalDate swipeDate) {
        this.userId = userId;
        this.actionType = actionType;
        this.like = like;
        this.dislike = dislike;
        this.swipeDate = swipeDate;
    }

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Like getLike() {
        return like;
    }

    public void setLike(Like like) {
        this.like = like;
    }

    public Dislike getDislike() {
        return dislike;
    }

    public void setDislike(Dislike dislike) {
        this.dislike = dislike;
    }

    public LocalDate getSwipeDate() {
        return swipeDate;
    }

    public void setSwipeDate(LocalDate swipeDate) {
        this.swipeDate = swipeDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSwipeEntity that = (UserSwipeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserSwipeEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", actionType=" + actionType +
                ", swipeDate=" + swipeDate +
                ", createdAt=" + createdAt +
                '}';
    }
}

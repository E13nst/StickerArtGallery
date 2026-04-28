package com.example.sticker_art_gallery.repository.stylefeed;

import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemLikeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StyleFeedItemLikeRepository extends JpaRepository<StyleFeedItemLikeEntity, Long> {

    boolean existsByUserIdAndStyleFeedItem_Id(Long userId, Long styleFeedItemId);

    Optional<StyleFeedItemLikeEntity> findByUserIdAndStyleFeedItem_Id(Long userId, Long styleFeedItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM StyleFeedItemLikeEntity l WHERE l.userId = :userId AND l.styleFeedItem.id = :itemId")
    Optional<StyleFeedItemLikeEntity> findByUserIdAndItemIdForUpdate(
            @Param("userId") Long userId,
            @Param("itemId") Long itemId);

    void deleteByUserIdAndStyleFeedItem_Id(Long userId, Long styleFeedItemId);
}

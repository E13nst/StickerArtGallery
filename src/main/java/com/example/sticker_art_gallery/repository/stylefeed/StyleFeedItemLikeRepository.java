package com.example.sticker_art_gallery.repository.stylefeed;

import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemLikeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Query("SELECT DISTINCT l.styleFeedItem.id FROM StyleFeedItemLikeEntity l WHERE l.userId = :userId")
    List<Long> findDistinctStyleFeedItemIdsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StyleFeedItemLikeEntity l WHERE l.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT user_id FROM style_feed_item_likes
                UNION
                SELECT user_id FROM style_feed_item_dislikes
            ) voters
            """, nativeQuery = true)
    long countDistinctVotersNative();
}

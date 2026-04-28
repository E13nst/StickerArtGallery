package com.example.sticker_art_gallery.repository.stylefeed;

import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemDislikeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StyleFeedItemDislikeRepository extends JpaRepository<StyleFeedItemDislikeEntity, Long> {

    boolean existsByUserIdAndStyleFeedItem_Id(Long userId, Long styleFeedItemId);

    Optional<StyleFeedItemDislikeEntity> findByUserIdAndStyleFeedItem_Id(Long userId, Long styleFeedItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM StyleFeedItemDislikeEntity d WHERE d.userId = :userId AND d.styleFeedItem.id = :itemId")
    Optional<StyleFeedItemDislikeEntity> findByUserIdAndItemIdForUpdate(
            @Param("userId") Long userId,
            @Param("itemId") Long itemId);

    void deleteByUserIdAndStyleFeedItem_Id(Long userId, Long styleFeedItemId);
}

package com.example.sticker_art_gallery.repository.stylefeed;

import com.example.sticker_art_gallery.model.stylefeed.CandidateFeedVisibility;
import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StyleFeedItemRepository extends JpaRepository<StyleFeedItemEntity, Long> {

    /**
     * Случайная видимая запись style feed, которую пользователь ещё не оценил.
     */
    @Query(value = """
            SELECT sf.* FROM style_feed_items sf
            WHERE (
                sf.admin_visibility_override = TRUE
                OR (sf.admin_visibility_override IS NULL AND sf.visibility = 'VISIBLE')
            )
            AND sf.id NOT IN (
                SELECT sfl.style_feed_item_id
                FROM style_feed_item_likes sfl
                WHERE sfl.user_id = :userId
            )
            AND sf.id NOT IN (
                SELECT sfd.style_feed_item_id
                FROM style_feed_item_dislikes sfd
                WHERE sfd.user_id = :userId
            )
            ORDER BY RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<StyleFeedItemEntity> findRandomNotRatedByUser(@Param("userId") Long userId);

    Optional<StyleFeedItemEntity> findByStylePreset_Id(Long stylePresetId);

    boolean existsByStylePreset_Id(Long stylePresetId);

    List<StyleFeedItemEntity> findAllByOrderByCreatedAtDesc();

    List<StyleFeedItemEntity> findByVisibilityOrderByCreatedAtDesc(CandidateFeedVisibility visibility);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sf FROM StyleFeedItemEntity sf WHERE sf.id = :id")
    Optional<StyleFeedItemEntity> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET likes_count = likes_count + 1,
                updated_at  = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void applyLike(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET dislikes_count = dislikes_count + 1,
                visibility = CASE
                    WHEN dislikes_count + 1 >= 7
                         AND likes_count + dislikes_count + 1 > 10
                         AND admin_visibility_override IS NULL
                    THEN 'AUTO_HIDDEN'
                    ELSE visibility
                END,
                updated_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void applyDislikeAndAutoHide(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET likes_count = GREATEST(likes_count - 1, 0),
                updated_at  = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void decrementLikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET dislikes_count = GREATEST(dislikes_count - 1, 0),
                visibility = CASE
                    WHEN visibility = 'AUTO_HIDDEN'
                         AND admin_visibility_override IS NULL
                         AND (GREATEST(dislikes_count - 1, 0) < 7
                              OR likes_count + GREATEST(dislikes_count - 1, 0) <= 10)
                    THEN 'VISIBLE'
                    ELSE visibility
                END,
                updated_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void decrementDislikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET admin_visibility_override = :override,
                visibility = CASE
                    WHEN :override = TRUE  THEN 'ADMIN_FORCED_VISIBLE'
                    WHEN :override = FALSE THEN 'ADMIN_HIDDEN'
                    ELSE CASE
                        WHEN dislikes_count >= 7 AND likes_count + dislikes_count > 10
                        THEN 'AUTO_HIDDEN'
                        ELSE 'VISIBLE'
                    END
                END,
                updated_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void setAdminVisibilityOverride(@Param("id") Long id, @Param("override") Boolean override);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO style_feed_items (
                task_id,
                cached_image_id,
                style_preset_id,
                preset_owner_user_id,
                likes_count,
                dislikes_count,
                visibility,
                admin_visibility_override,
                created_at,
                updated_at
            ) VALUES (
                :taskId,
                :cachedImageId,
                :stylePresetId,
                :presetOwnerUserId,
                0,
                0,
                'VISIBLE',
                NULL,
                NOW(),
                NOW()
            )
            ON CONFLICT (style_preset_id) WHERE style_preset_id IS NOT NULL DO NOTHING
            """, nativeQuery = true)
    int insertForStylePresetIfAbsent(@Param("taskId") String taskId,
                                     @Param("cachedImageId") UUID cachedImageId,
                                     @Param("stylePresetId") Long stylePresetId,
                                     @Param("presetOwnerUserId") Long presetOwnerUserId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET admin_visibility_override = FALSE,
                visibility = 'ADMIN_HIDDEN',
                updated_at = NOW()
            WHERE style_preset_id = :stylePresetId
            """, nativeQuery = true)
    int hideByStylePresetId(@Param("stylePresetId") Long stylePresetId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE style_feed_items
            SET admin_visibility_override = NULL,
                visibility = 'VISIBLE',
                updated_at = NOW()
            WHERE style_preset_id = :stylePresetId
            """, nativeQuery = true)
    int republishByStylePresetId(@Param("stylePresetId") Long stylePresetId);
}

package com.example.sticker_art_gallery.repository.meme;

import com.example.sticker_art_gallery.model.meme.MemeCandidateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemeCandidateRepository extends JpaRepository<MemeCandidateEntity, Long> {

    /**
     * Получить случайный VISIBLE мем-кандидат, который пользователь ещё не оценил.
     * Исключает кандидаты, по которым у пользователя уже есть строка в
     * meme_candidate_likes или meme_candidate_dislikes.
     * ADMIN_FORCED_VISIBLE тоже попадает в выдачу (admin_visibility_override = TRUE).
     */
    @Query(value = """
            SELECT mc.* FROM meme_candidates mc
            WHERE (
                mc.admin_visibility_override = TRUE
                OR (mc.admin_visibility_override IS NULL AND mc.visibility = 'VISIBLE')
            )
            AND mc.id NOT IN (
                SELECT mcl.meme_candidate_id
                FROM meme_candidate_likes mcl
                WHERE mcl.user_id = :userId
            )
            AND mc.id NOT IN (
                SELECT mcd.meme_candidate_id
                FROM meme_candidate_dislikes mcd
                WHERE mcd.user_id = :userId
            )
            ORDER BY RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<MemeCandidateEntity> findRandomNotRatedByUser(@Param("userId") Long userId);

    Optional<MemeCandidateEntity> findByStylePreset_Id(Long stylePresetId);

    boolean existsByStylePreset_Id(Long stylePresetId);

    /**
     * Pessimistic write lock для атомарного взаимоисключения лайк/дизлайк.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mc FROM MemeCandidateEntity mc WHERE mc.id = :id")
    Optional<MemeCandidateEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * Атомарный CAS-апдейт: инкрементирует likes_count и не меняет visibility.
     * Автоскрытие не применяется к лайкам.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE meme_candidates
            SET likes_count = likes_count + 1,
                updated_at  = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void applyLike(@Param("id") Long id);

    /**
     * Атомарный CAS-апдейт: инкрементирует dislikes_count и применяет автоскрытие
     * если dislikes_count + 1 >= 7 AND likes_count + dislikes_count + 1 > 10
     * AND admin_visibility_override IS NULL.
     * Логика исполняется целиком на уровне БД — нет read-then-write из Java.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE meme_candidates
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

    /**
     * Декремент likes_count при отмене лайка.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE meme_candidates
            SET likes_count = GREATEST(likes_count - 1, 0),
                updated_at  = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void decrementLikesCount(@Param("id") Long id);

    /**
     * Декремент dislikes_count при отмене дизлайка (и снятие AUTO_HIDDEN если нужно).
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE meme_candidates
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

    /**
     * Установить admin_visibility_override и пересчитать visibility.
     * override = TRUE → ADMIN_FORCED_VISIBLE; FALSE → ADMIN_HIDDEN; NULL → вернуть к автоправилу.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE meme_candidates
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
            INSERT INTO meme_candidates (
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
            UPDATE meme_candidates
            SET admin_visibility_override = FALSE,
                visibility = 'ADMIN_HIDDEN',
                updated_at = NOW()
            WHERE style_preset_id = :stylePresetId
            """, nativeQuery = true)
    int hideByStylePresetId(@Param("stylePresetId") Long stylePresetId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE meme_candidates
            SET admin_visibility_override = NULL,
                visibility = 'VISIBLE',
                updated_at = NOW()
            WHERE style_preset_id = :stylePresetId
            """, nativeQuery = true)
    int republishByStylePresetId(@Param("stylePresetId") Long stylePresetId);
}

package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.projection.UserProfileWithStickerCountsProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    /**
     * Найти профиль по userId (Telegram ID)
     */
    Optional<UserProfileEntity> findByUserId(Long userId);

    /**
     * Найти профиль по userId с блокировкой для обновления
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserProfileEntity up where up.userId = :userId")
    Optional<UserProfileEntity> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Проверить существование профиля по userId
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Найти все профили с базовыми фильтрами, пагинацией и счетчиками стикерсетов (для админ-панели)
     * Возвращает projection с дополнительными полями: ownedStickerSetsCount и authoredStickerSetsCount
     * Поддерживает сортировку по: createdAt, ownedStickerSetsCount, authoredStickerSetsCount
     */
    @Query(value = 
           "SELECT " +
           "  up.id, up.user_id AS userId, " +
           "  CAST(up.role AS TEXT) AS role, " +
           "  up.art_balance AS artBalance, " +
           "  up.is_blocked AS isBlocked, " +
           "  CAST(up.subscription_status AS TEXT) AS subscriptionStatus, " +
           "  up.created_at AS createdAt, up.updated_at AS updatedAt, " +
           "  COALESCE(oc.cnt, 0) AS ownedStickerSetsCount, " +
           "  COALESCE(ac.cnt, 0) AS authoredStickerSetsCount " +
           "FROM user_profiles up " +
           "LEFT JOIN users u ON up.user_id = u.id " +
           "LEFT JOIN (SELECT user_id, COUNT(*) AS cnt FROM stickersets WHERE state='ACTIVE' GROUP BY user_id) oc ON oc.user_id = up.user_id " +
           "LEFT JOIN (SELECT author_id, COUNT(*) AS cnt FROM stickersets WHERE state='ACTIVE' AND author_id IS NOT NULL GROUP BY author_id) ac ON ac.author_id = up.user_id " +
           "WHERE (:role IS NULL OR CAST(up.role AS TEXT) = :role) " +
           "  AND (:isBlocked IS NULL OR up.is_blocked = :isBlocked) " +
           "  AND (:search IS NULL OR :search = '' OR CAST(up.user_id AS TEXT) LIKE CONCAT('%', :search, '%') OR u.username ILIKE CONCAT('%', :search, '%')) " +
           "ORDER BY " +
           "  CASE WHEN :sort = 'createdAt' AND :direction = 'ASC' THEN up.created_at END ASC, " +
           "  CASE WHEN :sort = 'createdAt' AND :direction = 'DESC' THEN up.created_at END DESC, " +
           "  CASE WHEN :sort = 'ownedStickerSetsCount' AND :direction = 'ASC' THEN COALESCE(oc.cnt, 0) END ASC, " +
           "  CASE WHEN :sort = 'ownedStickerSetsCount' AND :direction = 'DESC' THEN COALESCE(oc.cnt, 0) END DESC, " +
           "  CASE WHEN :sort = 'authoredStickerSetsCount' AND :direction = 'ASC' THEN COALESCE(ac.cnt, 0) END ASC, " +
           "  CASE WHEN :sort = 'authoredStickerSetsCount' AND :direction = 'DESC' THEN COALESCE(ac.cnt, 0) END DESC, " +
           "  up.created_at DESC, up.user_id ASC",
           countQuery = 
           "SELECT COUNT(*) FROM user_profiles up " +
           "LEFT JOIN users u ON up.user_id = u.id " +
           "WHERE (:role IS NULL OR CAST(up.role AS TEXT) = :role) " +
           "  AND (:isBlocked IS NULL OR up.is_blocked = :isBlocked) " +
           "  AND (:search IS NULL OR :search = '' OR CAST(up.user_id AS TEXT) LIKE CONCAT('%', :search, '%') OR u.username ILIKE CONCAT('%', :search, '%'))",
           nativeQuery = true)
    Page<UserProfileWithStickerCountsProjection> findAllWithFiltersAndCounts(
            @Param("role") String role,
            @Param("isBlocked") Boolean isBlocked,
            @Param("search") String search,
            @Param("sort") String sort,
            @Param("direction") String direction,
            Pageable pageable
    );
}

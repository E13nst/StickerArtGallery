package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StickerSetRepository extends JpaRepository<StickerSet, Long> {
    
    List<StickerSet> findByUserId(Long userId);
    
    /**
     * Поиск стикерсетов пользователя с пагинацией
     */
    Page<StickerSet> findByUserId(Long userId, Pageable pageable);
    
    Optional<StickerSet> findByName(String name);
    
    /**
     * Поиск стикерсета по имени с игнорированием регистра
     */
    Optional<StickerSet> findByNameIgnoreCase(String name);
    
    StickerSet findByTitle(String title);
    
    /**
     * Поиск стикерсета по заголовку с игнорированием регистра
     */
    Optional<StickerSet> findByTitleIgnoreCase(String title);
    
    /**
     * Поиск стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys")
    Page<StickerSet> findByCategoryKeys(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
    
    /**
     * Поиск публичных и активных стикерсетов с пагинацией (для галереи)
     */
    @Query("SELECT ss FROM StickerSet ss WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC'")
    Page<StickerSet> findPublicAndActive(Pageable pageable);
    
    /**
     * Поиск публичных и активных стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC'")
    Page<StickerSet> findByCategoryKeysPublicAndActive(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
    
    /**
     * Публичные, активные стикерсеты с гибкой фильтрацией по type/userId/isVerified
     */
    @Query("SELECT ss FROM StickerSet ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' " +
           "AND (:type IS NULL OR ss.type = :type) " +
           "AND (:userId IS NULL OR ss.userId = :userId) " +
           "AND (:isVerified IS NULL OR :isVerified = false OR ss.isVerified = true)")
    Page<StickerSet> findPublicNotBlockedFiltered(@Param("type") StickerSetType type,
                                                   @Param("userId") Long userId,
                                                   @Param("isVerified") Boolean isVerified,
                                                   Pageable pageable);

    /**
     * Поиск только официальных, публичных и активных стикерсетов с пагинацией
     */
    @Query("SELECT ss FROM StickerSet ss WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' AND ss.type = 'OFFICIAL'")
    Page<StickerSet> findPublicNotBlockedAndOfficial(Pageable pageable);
    
    /**
     * Поиск публичных и активных стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC'")
    Page<StickerSet> findByCategoryKeysPublicAndNotBlocked(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
    
    /**
     * Публичные, активные по категориям с гибкой фильтрацией по type/userId/isVerified
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' " +
           "AND (:type IS NULL OR ss.type = :type) " +
           "AND (:userId IS NULL OR ss.userId = :userId) " +
           "AND (:isVerified IS NULL OR :isVerified = false OR ss.isVerified = true)")
    Page<StickerSet> findByCategoryKeysPublicNotBlockedFiltered(@Param("categoryKeys") String[] categoryKeys,
                                                                @Param("type") StickerSetType type,
                                                                @Param("userId") Long userId,
                                                                @Param("isVerified") Boolean isVerified,
                                                                Pageable pageable);
    
    /**
     * Поиск официальных, публичных и активных стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' AND ss.type = 'OFFICIAL'")
    Page<StickerSet> findByCategoryKeysPublicNotBlockedAndOfficial(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StickerSet ss set ss.likesCount = ss.likesCount + 1 where ss.id = :id")
    int incrementLikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StickerSet ss set ss.likesCount = CASE WHEN ss.likesCount > 0 THEN ss.likesCount - 1 ELSE 0 END where ss.id = :id")
    int decrementLikesCount(@Param("id") Long id);

    /**
     * Пересчитать likes_count на основе реального количества записей в таблице likes
     * Используется для исправления расхождений между денормализованным полем и реальными данными
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE stickersets s " +
           "SET likes_count = COALESCE((" +
           "  SELECT COUNT(*) FROM likes l WHERE l.stickerset_id = s.id" +
           "), 0) " +
           "WHERE s.id = :id", nativeQuery = true)
    int recalculateLikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StickerSet ss set ss.dislikesCount = ss.dislikesCount + 1 where ss.id = :id")
    int incrementDislikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StickerSet ss set ss.dislikesCount = CASE WHEN ss.dislikesCount > 0 THEN ss.dislikesCount - 1 ELSE 0 END where ss.id = :id")
    int decrementDislikesCount(@Param("id") Long id);

    /**
     * Пересчитать dislikes_count на основе реального количества записей в таблице dislikes
     * Используется для исправления расхождений между денормализованным полем и реальными данными
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE stickersets s " +
           "SET dislikes_count = COALESCE((" +
           "  SELECT COUNT(*) FROM dislikes d WHERE d.stickerset_id = s.id" +
           "), 0) " +
           "WHERE s.id = :id", nativeQuery = true)
    int recalculateDislikesCount(@Param("id") Long id);

    /**
     * Поиск стикерсетов пользователя с дополнительными фильтрами
     * @param includeBlocked если true, включает заблокированные стикерсеты (state = 'BLOCKED')
     *                       для владельца и админа, чтобы они видели свои заблокированные наборы
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "LEFT JOIN ss.categories c " +
           "WHERE ss.userId = :userId " +
           "AND (ss.state = 'ACTIVE' OR (:includeBlocked = true AND ss.state = 'BLOCKED')) " +
           "AND (:visibilityFilter = 'ALL' OR " +
           "     (:visibilityFilter = 'PUBLIC' AND ss.visibility = 'PUBLIC') OR " +
           "     (:visibilityFilter = 'PRIVATE' AND ss.visibility = 'PRIVATE')) " +
           "AND (:type IS NULL OR ss.type = :type) " +
           "AND (:isVerified IS NULL OR :isVerified = false OR ss.isVerified = true) " +
           "AND (:categoryKeys IS NULL OR c.key IN :categoryKeys) " +
           "AND (:likedOnly = false OR EXISTS (" +
           "   SELECT 1 FROM Like l WHERE l.userId = :currentUserId AND l.stickerSet = ss" +
           "))")
    Page<StickerSet> findUserStickerSetsFiltered(@Param("userId") Long userId,
                                                 @Param("visibilityFilter") String visibilityFilter,
                                                 @Param("type") StickerSetType type,
                                                 @Param("isVerified") Boolean isVerified,
                                                 @Param("categoryKeys") Set<String> categoryKeys,
                                                 @Param("likedOnly") boolean likedOnly,
                                                 @Param("currentUserId") Long currentUserId,
                                                 @Param("includeBlocked") boolean includeBlocked,
                                                 Pageable pageable);

    /**
     * Поиск верифицированных стикерсетов владельца (deprecated authorId => userId + isVerified)
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "LEFT JOIN ss.categories c " +
           "WHERE ss.userId = :userId AND ss.isVerified = true " +
           "AND ss.state = 'ACTIVE' " +
           "AND (:visibilityFilter = 'ALL' OR " +
           "     (:visibilityFilter = 'PUBLIC' AND ss.visibility = 'PUBLIC') OR " +
           "     (:visibilityFilter = 'PRIVATE' AND ss.visibility = 'PRIVATE')) " +
           "AND (:type IS NULL OR ss.type = :type) " +
           "AND (:categoryKeys IS NULL OR c.key IN :categoryKeys)")
    Page<StickerSet> findVerifiedOwnerStickerSetsFiltered(@Param("userId") Long userId,
                                                          @Param("visibilityFilter") String visibilityFilter,
                                                          @Param("type") StickerSetType type,
                                                          @Param("categoryKeys") Set<String> categoryKeys,
                                                          Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime createdAfter);

    @Query("SELECT DISTINCT ss.userId FROM StickerSet ss WHERE ss.createdAt >= :since")
    List<Long> findDistinctUserIdsByCreatedAtAfter(@Param("since") LocalDateTime since);

    /**
     * Подсчитать количество стикерсетов по видимости и времени создания
     */
    long countByVisibilityAndCreatedAtAfter(StickerSetVisibility visibility, LocalDateTime createdAfter);

    /**
     * Подсчитать общее количество стикерсетов по видимости
     */
    long countByVisibility(StickerSetVisibility visibility);

    /**
     * Получить топ пользователей по количеству созданных стикерсетов (общая статистика)
     * Сортировка по totalCount
     */
    @Query(value = "SELECT ss.user_id, " +
           "COUNT(ss.id) as total_count, " +
           "SUM(CASE WHEN ss.visibility = 'PUBLIC' THEN 1 ELSE 0 END) as public_count, " +
           "SUM(CASE WHEN ss.visibility = 'PRIVATE' THEN 1 ELSE 0 END) as private_count " +
           "FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' " +
           "GROUP BY ss.user_id " +
           "ORDER BY total_count DESC, ss.user_id ASC",
           nativeQuery = true)
    Page<Object[]> findTopUsersByTotalStickerSetCount(Pageable pageable);

    /**
     * Получить топ пользователей по количеству созданных публичных стикерсетов
     * Сортировка по количеству публичных стикерсетов
     */
    @Query(value = "SELECT ss.user_id, " +
           "(SELECT COUNT(s2.id) FROM stickersets s2 WHERE s2.user_id = ss.user_id AND s2.state = 'ACTIVE') as total_count, " +
           "COUNT(ss.id) as public_count, " +
           "(SELECT COUNT(s3.id) FROM stickersets s3 WHERE s3.user_id = ss.user_id AND s3.state = 'ACTIVE' AND s3.visibility = 'PRIVATE') as private_count " +
           "FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' " +
           "GROUP BY ss.user_id " +
           "ORDER BY public_count DESC, ss.user_id ASC",
           nativeQuery = true)
    Page<Object[]> findTopUsersByPublicStickerSetCount(Pageable pageable);

    /**
     * Получить топ пользователей по количеству созданных приватных стикерсетов
     * Сортировка по количеству приватных стикерсетов
     */
    @Query(value = "SELECT ss.user_id, " +
           "(SELECT COUNT(s2.id) FROM stickersets s2 WHERE s2.user_id = ss.user_id AND s2.state = 'ACTIVE') as total_count, " +
           "(SELECT COUNT(s3.id) FROM stickersets s3 WHERE s3.user_id = ss.user_id AND s3.state = 'ACTIVE' AND s3.visibility = 'PUBLIC') as public_count, " +
           "COUNT(ss.id) as private_count " +
           "FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PRIVATE' " +
           "GROUP BY ss.user_id " +
           "ORDER BY private_count DESC, ss.user_id ASC",
           nativeQuery = true)
    Page<Object[]> findTopUsersByPrivateStickerSetCount(Pageable pageable);
    
    /**
     * Топ верифицированных авторов (user_id, is_verified=true) по количеству стикерсетов
     */
    @Query(value = "SELECT ss.user_id, " +
           "COUNT(ss.id) as total_count, " +
           "SUM(CASE WHEN ss.visibility = 'PUBLIC' THEN 1 ELSE 0 END) as public_count, " +
           "SUM(CASE WHEN ss.visibility = 'PRIVATE' THEN 1 ELSE 0 END) as private_count " +
           "FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.is_verified = TRUE " +
           "GROUP BY ss.user_id " +
           "ORDER BY total_count DESC, ss.user_id ASC",
           countQuery = "SELECT COUNT(DISTINCT ss.user_id) FROM stickersets ss " +
                        "WHERE ss.state = 'ACTIVE' AND ss.is_verified = TRUE",
           nativeQuery = true)
    Page<Object[]> findTopAuthorsByTotalStickerSetCount(Pageable pageable);

    @Query(value = "SELECT ss.user_id, " +
           "(SELECT COUNT(s2.id) FROM stickersets s2 WHERE s2.user_id = ss.user_id AND s2.state = 'ACTIVE' AND s2.is_verified = TRUE) as total_count, " +
           "COUNT(ss.id) as public_count, " +
           "(SELECT COUNT(s3.id) FROM stickersets s3 WHERE s3.user_id = ss.user_id AND s3.state = 'ACTIVE' AND s3.is_verified = TRUE AND s3.visibility = 'PRIVATE') as private_count " +
           "FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' AND ss.is_verified = TRUE " +
           "GROUP BY ss.user_id " +
           "ORDER BY public_count DESC, ss.user_id ASC",
           countQuery = "SELECT COUNT(DISTINCT ss.user_id) FROM stickersets ss " +
                        "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' AND ss.is_verified = TRUE",
           nativeQuery = true)
    Page<Object[]> findTopAuthorsByPublicStickerSetCount(Pageable pageable);

    @Query(value = "SELECT ss.user_id, " +
           "(SELECT COUNT(s2.id) FROM stickersets s2 WHERE s2.user_id = ss.user_id AND s2.state = 'ACTIVE' AND s2.is_verified = TRUE) as total_count, " +
           "(SELECT COUNT(s3.id) FROM stickersets s3 WHERE s3.user_id = ss.user_id AND s3.state = 'ACTIVE' AND s3.is_verified = TRUE AND s3.visibility = 'PUBLIC') as public_count, " +
           "COUNT(ss.id) as private_count " +
           "FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PRIVATE' AND ss.is_verified = TRUE " +
           "GROUP BY ss.user_id " +
           "ORDER BY private_count DESC, ss.user_id ASC",
           countQuery = "SELECT COUNT(DISTINCT ss.user_id) FROM stickersets ss " +
                        "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PRIVATE' AND ss.is_verified = TRUE",
           nativeQuery = true)
    Page<Object[]> findTopAuthorsByPrivateStickerSetCount(Pageable pageable);
    
    /**
     * Поиск публичных активных стикерсетов по title или description с фильтрацией
     * Ищет также по многоязычным описаниям из таблицы stickerset_descriptions
     * Использует LEFT JOIN FETCH для предотвращения N+1 проблемы с descriptions
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "LEFT JOIN FETCH ss.descriptions sd " +
           "LEFT JOIN ss.categories c " +
           "WHERE (LOWER(ss.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "       OR LOWER(ss.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "       OR LOWER(sd.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND ss.state = 'ACTIVE' " +
           "AND ss.visibility = 'PUBLIC' " +
           "AND (:categoryKeys IS NULL OR c.key IN :categoryKeys) " +
           "AND (:type IS NULL OR ss.type = :type) " +
           "AND (:userId IS NULL OR ss.userId = :userId) " +
           "AND (:isVerified IS NULL OR :isVerified = false OR ss.isVerified = true)")
    Page<StickerSet> searchPublicStickerSets(@Param("query") String query,
                                             @Param("categoryKeys") Set<String> categoryKeys,
                                             @Param("type") StickerSetType type,
                                             @Param("userId") Long userId,
                                             @Param("isVerified") Boolean isVerified,
                                             Pageable pageable);
    
    /**
     * Получить случайный стикерсет, который пользователь еще не лайкал и не дизлайкал
     * Исключает стикерсеты, на которые пользователь уже поставил лайк или дизлайк
     */
    @Query(value = "SELECT ss.* FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' " +
           "AND ss.visibility = 'PUBLIC' " +
           "AND ss.id NOT IN (" +
           "  SELECT l.stickerset_id FROM likes l WHERE l.user_id = :userId" +
           ") " +
           "AND ss.id NOT IN (" +
           "  SELECT d.stickerset_id FROM dislikes d WHERE d.user_id = :userId" +
           ") " +
           "ORDER BY RANDOM() " +
           "LIMIT 1",
           nativeQuery = true)
    Optional<StickerSet> findRandomStickerSetNotRatedByUser(@Param("userId") Long userId);
    
    /**
     * Получить страницу случайных стикерсетов, которые пользователь еще не лайкал и не дизлайкал
     * Исключает стикерсеты, на которые пользователь уже поставил лайк или дизлайк
     */
    @Query(value = "SELECT ss.* FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' " +
           "AND ss.visibility = 'PUBLIC' " +
           "AND ss.id NOT IN (" +
           "  SELECT l.stickerset_id FROM likes l WHERE l.user_id = :userId" +
           ") " +
           "AND ss.id NOT IN (" +
           "  SELECT d.stickerset_id FROM dislikes d WHERE d.user_id = :userId" +
           ") " +
           "ORDER BY RANDOM()",
           countQuery = "SELECT COUNT(ss.id) FROM stickersets ss " +
           "WHERE ss.state = 'ACTIVE' " +
           "AND ss.visibility = 'PUBLIC' " +
           "AND ss.id NOT IN (" +
           "  SELECT l.stickerset_id FROM likes l WHERE l.user_id = :userId" +
           ") " +
           "AND ss.id NOT IN (" +
           "  SELECT d.stickerset_id FROM dislikes d WHERE d.user_id = :userId" +
           ")",
           nativeQuery = true)
    Page<StickerSet> findRandomStickerSetsNotRatedByUser(@Param("userId") Long userId, Pageable pageable);
}

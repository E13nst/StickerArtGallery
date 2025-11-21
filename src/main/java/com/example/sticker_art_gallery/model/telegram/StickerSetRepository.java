package com.example.sticker_art_gallery.model.telegram;

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
     * Публичные, активные стикерсеты с гибкой фильтрацией по type/author/userId
     */
    @Query("SELECT ss FROM StickerSet ss " +
           "WHERE ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' " +
           "AND (:officialOnly = false OR ss.type = 'OFFICIAL') " +
           "AND (:authorId IS NULL OR ss.authorId = :authorId) " +
           "AND (:hasAuthorOnly = false OR ss.authorId IS NOT NULL) " +
           "AND (:userId IS NULL OR ss.userId = :userId)")
    Page<StickerSet> findPublicNotBlockedFiltered(@Param("officialOnly") boolean officialOnly,
                                                   @Param("authorId") Long authorId,
                                                   @Param("hasAuthorOnly") boolean hasAuthorOnly,
                                                   @Param("userId") Long userId,
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
     * Публичные, активные по категориям с гибкой фильтрацией по type/author/userId
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.state = 'ACTIVE' AND ss.visibility = 'PUBLIC' " +
           "AND (:officialOnly = false OR ss.type = 'OFFICIAL') " +
           "AND (:authorId IS NULL OR ss.authorId = :authorId) " +
           "AND (:hasAuthorOnly = false OR ss.authorId IS NOT NULL) " +
           "AND (:userId IS NULL OR ss.userId = :userId)")
    Page<StickerSet> findByCategoryKeysPublicNotBlockedFiltered(@Param("categoryKeys") String[] categoryKeys,
                                                                @Param("officialOnly") boolean officialOnly,
                                                                @Param("authorId") Long authorId,
                                                                @Param("hasAuthorOnly") boolean hasAuthorOnly,
                                                                @Param("userId") Long userId,
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

    /**
     * Поиск стикерсетов пользователя с дополнительными фильтрами
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "LEFT JOIN ss.categories c " +
           "WHERE ss.userId = :userId " +
           "AND ss.state = 'ACTIVE' " +
           "AND (:visibilityFilter = 'ALL' OR " +
           "     (:visibilityFilter = 'PUBLIC' AND ss.visibility = 'PUBLIC') OR " +
           "     (:visibilityFilter = 'PRIVATE' AND ss.visibility = 'PRIVATE')) " +
           "AND (:hasAuthorOnly = false OR ss.authorId IS NOT NULL) " +
           "AND (:categoryKeys IS NULL OR c.key IN :categoryKeys) " +
           "AND (:likedOnly = false OR EXISTS (" +
           "   SELECT 1 FROM Like l WHERE l.userId = :currentUserId AND l.stickerSet = ss" +
           "))")
    Page<StickerSet> findUserStickerSetsFiltered(@Param("userId") Long userId,
                                                 @Param("visibilityFilter") String visibilityFilter,
                                                 @Param("hasAuthorOnly") boolean hasAuthorOnly,
                                                 @Param("categoryKeys") Set<String> categoryKeys,
                                                 @Param("likedOnly") boolean likedOnly,
                                                 @Param("currentUserId") Long currentUserId,
                                                 Pageable pageable);

    /**
     * Поиск авторских стикерсетов с дополнительными фильтрами
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "LEFT JOIN ss.categories c " +
           "WHERE ss.authorId = :authorId " +
           "AND ss.state = 'ACTIVE' " +
           "AND (:visibilityFilter = 'ALL' OR " +
           "     (:visibilityFilter = 'PUBLIC' AND ss.visibility = 'PUBLIC') OR " +
           "     (:visibilityFilter = 'PRIVATE' AND ss.visibility = 'PRIVATE')) " +
           "AND (:categoryKeys IS NULL OR c.key IN :categoryKeys)")
    Page<StickerSet> findAuthorStickerSetsFiltered(@Param("authorId") Long authorId,
                                                   @Param("visibilityFilter") String visibilityFilter,
                                                   @Param("categoryKeys") Set<String> categoryKeys,
                                                   Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime createdAfter);

    @Query("SELECT DISTINCT ss.userId FROM StickerSet ss WHERE ss.createdAt >= :since")
    List<Long> findDistinctUserIdsByCreatedAtAfter(@Param("since") LocalDateTime since);
} 
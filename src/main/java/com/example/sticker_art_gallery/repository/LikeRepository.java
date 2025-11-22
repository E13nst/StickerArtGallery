package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository для работы с лайками стикерсетов
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    
    /**
     * Проверить существование лайка от пользователя на стикерсет
     */
    boolean existsByUserIdAndStickerSetId(Long userId, Long stickerSetId);
    
    /**
     * Получить лайк от пользователя на стикерсет
     */
    Optional<Like> findByUserIdAndStickerSetId(Long userId, Long stickerSetId);
    
    /**
     * Удалить лайк от пользователя на стикерсет
     */
    void deleteByUserIdAndStickerSetId(Long userId, Long stickerSetId);
    
    /**
     * Подсчитать количество лайков стикерсета
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.stickerSet.id = :stickerSetId")
    long countByStickerSetId(@Param("stickerSetId") Long stickerSetId);
    
    /**
     * Получить стикерсеты, лайкнутые пользователем
     * Сортировка управляется через Pageable
     */
    @Query("SELECT s FROM StickerSet s WHERE s.id IN " +
           "(SELECT l.stickerSet.id FROM Like l WHERE l.userId = :userId)")
    List<StickerSet> findLikedStickerSetsByUserId(@Param("userId") Long userId);
    
    /**
     * Получить стикерсеты, лайкнутые пользователем с пагинацией
     * Сортировка управляется через Pageable
     */
    @Query("SELECT s FROM StickerSet s WHERE s.id IN " +
           "(SELECT l.stickerSet.id FROM Like l WHERE l.userId = :userId)")
    Page<StickerSet> findLikedStickerSetsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Получить топ стикерсетов по количеству лайков
     */
    @Query("SELECT s, COUNT(l) as likesCount FROM StickerSet s " +
           "LEFT JOIN s.likes l " +
           "WHERE s.state = com.example.sticker_art_gallery.model.telegram.StickerSetState.ACTIVE " +
           "AND s.visibility = com.example.sticker_art_gallery.model.telegram.StickerSetVisibility.PUBLIC " +
           "GROUP BY s.id " +
           "ORDER BY likesCount DESC, s.createdAt DESC")
    Page<Object[]> findTopStickerSetsByLikes(Pageable pageable);
    
    /**
     * Получить топ официальных стикерсетов по количеству лайков (только публичные и не заблокированные)
     */
    @Query("SELECT s, COUNT(l) as likesCount FROM StickerSet s " +
           "LEFT JOIN s.likes l " +
           "WHERE s.state = com.example.sticker_art_gallery.model.telegram.StickerSetState.ACTIVE " +
           "AND s.visibility = com.example.sticker_art_gallery.model.telegram.StickerSetVisibility.PUBLIC " +
           "AND s.type = com.example.sticker_art_gallery.model.telegram.StickerSetType.OFFICIAL " +
           "GROUP BY s.id " +
           "ORDER BY likesCount DESC, s.createdAt DESC")
    Page<Object[]> findTopOfficialStickerSetsByLikes(Pageable pageable);

    /**
     * Получить топ стикерсетов по лайкам с фильтрами officialOnly/authorId/hasAuthorOnly
     */
    @Query("SELECT s, COUNT(l) as likesCount FROM StickerSet s " +
           "LEFT JOIN s.likes l " +
           "WHERE s.state = com.example.sticker_art_gallery.model.telegram.StickerSetState.ACTIVE " +
           "AND s.visibility = com.example.sticker_art_gallery.model.telegram.StickerSetVisibility.PUBLIC " +
           "AND (:officialOnly = false OR s.type = com.example.sticker_art_gallery.model.telegram.StickerSetType.OFFICIAL) " +
           "AND (:authorId IS NULL OR s.authorId = :authorId) " +
           "AND (:hasAuthorOnly = false OR s.authorId IS NOT NULL) " +
           "GROUP BY s.id " +
           "ORDER BY likesCount DESC, s.createdAt DESC")
    Page<Object[]> findTopStickerSetsByLikesFiltered(@Param("officialOnly") boolean officialOnly,
                                                     @Param("authorId") Long authorId,
                                                     @Param("hasAuthorOnly") boolean hasAuthorOnly,
                                                     Pageable pageable);
    
    /**
     * Получить все лайки стикерсета
     */
    List<Like> findByStickerSetId(Long stickerSetId);
    
    /**
     * Получить все лайки пользователя
     */
    List<Like> findByUserId(Long userId);
    
    /**
     * Получить лайки пользователя с пагинацией
     */
    Page<Like> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Проверить, лайкнул ли пользователь любой из стикерсетов
     */
    @Query("SELECT l.stickerSet.id FROM Like l WHERE l.userId = :userId AND l.stickerSet.id IN :stickerSetIds")
    List<Long> findLikedStickerSetIdsByUserId(@Param("userId") Long userId, @Param("stickerSetIds") List<Long> stickerSetIds);
    
    /**
     * Получить лайкнутые стикерсеты пользователя по категориям
     * Сортировка управляется через Pageable
     */
    @Query("SELECT s FROM StickerSet s " +
           "JOIN s.categories c " +
           "WHERE s.id IN (SELECT l.stickerSet.id FROM Like l WHERE l.userId = :userId) " +
           "AND c.key IN :categoryKeys")
    Page<StickerSet> findLikedStickerSetsByUserIdAndCategoryKeys(
            @Param("userId") Long userId, 
            @Param("categoryKeys") List<String> categoryKeys, 
            Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime createdAfter);

    @Query("SELECT DISTINCT l.userId FROM Like l WHERE l.createdAt >= :since")
    List<Long> findDistinctUserIdsByCreatedAtAfter(@Param("since") LocalDateTime since);
    
    /**
     * Поиск лайкнутых стикерсетов пользователя по title или description
     */
    @Query("SELECT DISTINCT ss FROM Like l " +
           "JOIN l.stickerSet ss " +
           "LEFT JOIN ss.categories c " +
           "WHERE l.userId = :userId " +
           "AND (LOWER(ss.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(ss.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND ss.state = com.example.sticker_art_gallery.model.telegram.StickerSetState.ACTIVE " +
           "AND ss.visibility = com.example.sticker_art_gallery.model.telegram.StickerSetVisibility.PUBLIC " +
           "AND (:categoryKeys IS NULL OR c.key IN :categoryKeys)")
    Page<StickerSet> searchLikedStickerSets(@Param("userId") Long userId,
                                            @Param("query") String query,
                                            @Param("categoryKeys") Set<String> categoryKeys,
                                            Pageable pageable);
}

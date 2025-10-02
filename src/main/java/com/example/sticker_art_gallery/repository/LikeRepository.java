package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     */
    @Query("SELECT DISTINCT l.stickerSet FROM Like l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    List<StickerSet> findLikedStickerSetsByUserId(@Param("userId") Long userId);
    
    /**
     * Получить стикерсеты, лайкнутые пользователем с пагинацией
     */
    @Query("SELECT DISTINCT l.stickerSet FROM Like l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<StickerSet> findLikedStickerSetsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Получить топ стикерсетов по количеству лайков
     */
    @Query("SELECT s, COUNT(l) as likesCount FROM StickerSet s " +
           "LEFT JOIN s.likes l " +
           "GROUP BY s.id " +
           "ORDER BY likesCount DESC, s.createdAt DESC")
    Page<Object[]> findTopStickerSetsByLikes(Pageable pageable);
    
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
}

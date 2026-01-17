package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.Dislike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с дизлайками стикерсетов
 */
@Repository
public interface DislikeRepository extends JpaRepository<Dislike, Long> {
    
    /**
     * Проверить существование дизлайка от пользователя на стикерсет
     */
    boolean existsByUserIdAndStickerSetId(Long userId, Long stickerSetId);
    
    /**
     * Получить дизлайк от пользователя на стикерсет
     */
    Optional<Dislike> findByUserIdAndStickerSetId(Long userId, Long stickerSetId);
    
    /**
     * Удалить дизлайк от пользователя на стикерсет
     */
    void deleteByUserIdAndStickerSetId(Long userId, Long stickerSetId);
    
    /**
     * Подсчитать количество дизлайков стикерсета
     */
    @Query("SELECT COUNT(d) FROM Dislike d WHERE d.stickerSet.id = :stickerSetId")
    long countByStickerSetId(@Param("stickerSetId") Long stickerSetId);
    
    /**
     * Получить все дизлайки стикерсета
     */
    List<Dislike> findByStickerSetId(Long stickerSetId);
    
    /**
     * Получить все дизлайки пользователя
     */
    List<Dislike> findByUserId(Long userId);
    
    /**
     * Получить дизлайки пользователя с пагинацией
     */
    Page<Dislike> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Проверить, дизлайкнул ли пользователь любой из стикерсетов
     */
    @Query("SELECT d.stickerSet.id FROM Dislike d WHERE d.userId = :userId AND d.stickerSet.id IN :stickerSetIds")
    List<Long> findDislikedStickerSetIdsByUserId(@Param("userId") Long userId, @Param("stickerSetIds") List<Long> stickerSetIds);
}

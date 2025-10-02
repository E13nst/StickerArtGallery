package com.example.sticker_art_gallery.model.telegram;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerSetRepository extends JpaRepository<StickerSet, Long> {
    
    List<StickerSet> findByUserId(Long userId);
    
    /**
     * Поиск стикерсетов пользователя с пагинацией
     */
    Page<StickerSet> findByUserId(Long userId, Pageable pageable);
    
    Optional<StickerSet> findByName(String name);
    
    StickerSet findByTitle(String title);
    
    /**
     * Поиск стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys")
    Page<StickerSet> findByCategoryKeys(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
} 
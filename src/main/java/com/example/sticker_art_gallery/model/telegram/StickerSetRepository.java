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
     * Поиск только публичных стикерсетов с пагинацией
     */
    Page<StickerSet> findByIsPublic(Boolean isPublic, Pageable pageable);
    
    /**
     * Поиск публичных стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.isPublic = true")
    Page<StickerSet> findByCategoryKeysAndIsPublic(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
    
    /**
     * Поиск публичных и не заблокированных стикерсетов с пагинацией
     */
    @Query("SELECT ss FROM StickerSet ss WHERE ss.isPublic = true AND ss.isBlocked = false")
    Page<StickerSet> findPublicAndNotBlocked(Pageable pageable);
    
    /**
     * Поиск только официальных, публичных и не заблокированных стикерсетов с пагинацией
     */
    @Query("SELECT ss FROM StickerSet ss WHERE ss.isPublic = true AND ss.isBlocked = false AND ss.isOfficial = true")
    Page<StickerSet> findPublicNotBlockedAndOfficial(Pageable pageable);
    
    /**
     * Поиск публичных и не заблокированных стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.isPublic = true AND ss.isBlocked = false")
    Page<StickerSet> findByCategoryKeysPublicAndNotBlocked(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
    
    /**
     * Поиск официальных, публичных и не заблокированных стикерсетов по ключам категорий с пагинацией
     */
    @Query("SELECT DISTINCT ss FROM StickerSet ss " +
           "JOIN ss.categories c " +
           "WHERE c.key IN :categoryKeys AND ss.isPublic = true AND ss.isBlocked = false AND ss.isOfficial = true")
    Page<StickerSet> findByCategoryKeysPublicNotBlockedAndOfficial(@Param("categoryKeys") String[] categoryKeys, Pageable pageable);
} 
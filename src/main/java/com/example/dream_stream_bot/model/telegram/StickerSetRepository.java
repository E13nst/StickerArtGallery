package com.example.dream_stream_bot.model.telegram;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
} 
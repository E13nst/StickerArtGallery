package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.telegram.StickerSetTelegramCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StickerSetTelegramCacheRepository extends JpaRepository<StickerSetTelegramCacheEntity, Long> {
}

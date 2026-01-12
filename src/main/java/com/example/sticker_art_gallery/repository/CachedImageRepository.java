package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository для работы с кэшированными изображениями.
 */
@Repository
public interface CachedImageRepository extends JpaRepository<CachedImageEntity, UUID> {

    /**
     * Находит изображение по оригинальному URL.
     */
    Optional<CachedImageEntity> findByOriginalUrl(String originalUrl);

    /**
     * Находит изображение по имени файла.
     */
    Optional<CachedImageEntity> findByFileName(String fileName);

    /**
     * Находит все просроченные изображения.
     */
    @Query("SELECT c FROM CachedImageEntity c WHERE c.expiresAt < :now")
    List<CachedImageEntity> findExpired(@Param("now") OffsetDateTime now);

    /**
     * Удаляет все просроченные изображения и возвращает количество удаленных записей.
     */
    @Modifying
    @Query("DELETE FROM CachedImageEntity c WHERE c.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);

    /**
     * Проверяет, существует ли изображение с данным оригинальным URL.
     */
    boolean existsByOriginalUrl(String originalUrl);
}

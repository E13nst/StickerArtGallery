package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.swipe.SwipeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для работы с конфигурацией системы свайпов.
 * Singleton - только одна запись с id=1.
 */
@Repository
public interface SwipeConfigRepository extends JpaRepository<SwipeConfigEntity, Long> {
    // Используем стандартный метод findById из JpaRepository для получения конфигурации с id=1
}

package com.example.sticker_art_gallery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Конфигурация JPA репозиториев
 * Использует безопасный базовый класс для StickerSetRepository
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.sticker_art_gallery",
    repositoryBaseClass = com.example.sticker_art_gallery.model.telegram.SafeStickerSetRepositoryBase.class
)
public class JpaConfig {
}


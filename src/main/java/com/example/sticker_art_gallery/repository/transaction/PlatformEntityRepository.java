package com.example.sticker_art_gallery.repository.transaction;

import com.example.sticker_art_gallery.model.transaction.EntityType;
import com.example.sticker_art_gallery.model.transaction.PlatformEntityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для работы с универсальными сущностями
 */
@Repository
public interface PlatformEntityRepository extends JpaRepository<PlatformEntityEntity, Long> {

    /**
     * Найти сущность по типу и ссылке
     */
    Optional<PlatformEntityEntity> findByEntityTypeAndEntityRef(EntityType entityType, String entityRef);

    /**
     * Проверить существование сущности
     */
    boolean existsByEntityTypeAndEntityRef(EntityType entityType, String entityRef);
}

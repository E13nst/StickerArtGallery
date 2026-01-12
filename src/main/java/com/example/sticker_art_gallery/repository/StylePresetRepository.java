package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StylePresetRepository extends JpaRepository<StylePresetEntity, Long> {

    /**
     * Находит все доступные пресеты для пользователя:
     * - глобальные пресеты (is_global = true)
     * - персональные пресеты пользователя (owner_id = userId)
     * Только активные (is_enabled = true)
     */
    @Query("SELECT sp FROM StylePresetEntity sp WHERE " +
           "(sp.isGlobal = true OR sp.owner.userId = :userId) AND sp.isEnabled = true " +
           "ORDER BY sp.sortOrder ASC, sp.name ASC")
    List<StylePresetEntity> findAvailableForUser(Long userId);

    /**
     * Находит все глобальные пресеты (для админа)
     */
    @Query("SELECT sp FROM StylePresetEntity sp WHERE sp.isGlobal = true ORDER BY sp.sortOrder ASC, sp.name ASC")
    List<StylePresetEntity> findAllGlobal();

    /**
     * Находит все персональные пресеты пользователя
     */
    @Query("SELECT sp FROM StylePresetEntity sp WHERE sp.owner.userId = :userId ORDER BY sp.sortOrder ASC, sp.name ASC")
    List<StylePresetEntity> findByOwnerUserId(Long userId);

    /**
     * Находит пресет по коду и владельцу (для проверки уникальности)
     */
    Optional<StylePresetEntity> findByCodeAndOwner_UserId(String code, Long ownerId);

    /**
     * Находит глобальный пресет по коду
     */
    Optional<StylePresetEntity> findByCodeAndIsGlobalTrue(String code);

    /**
     * Проверяет существование пресета по ID и владельцу
     */
    @Query("SELECT COUNT(sp) > 0 FROM StylePresetEntity sp WHERE sp.id = :id AND " +
           "(sp.isGlobal = true OR sp.owner.userId = :userId)")
    boolean existsByIdAndAccessibleByUser(Long id, Long userId);
}

package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query("SELECT sp FROM StylePresetEntity sp JOIN FETCH sp.category c WHERE " +
           "(sp.isGlobal = true OR sp.owner.userId = :userId OR sp.publishedToCatalog = true) AND sp.isEnabled = true " +
           "ORDER BY c.sortOrder ASC, sp.sortOrder ASC, sp.name ASC")
    List<StylePresetEntity> findAvailableForUser(Long userId);

    @Query("SELECT sp FROM StylePresetEntity sp JOIN FETCH sp.category c " +
            "LEFT JOIN FETCH sp.previewImage " +
            "LEFT JOIN FETCH sp.referenceImage " +
            "WHERE (sp.isGlobal = true OR sp.owner.userId = :userId OR sp.publishedToCatalog = true) AND sp.isEnabled = true " +
            "ORDER BY c.sortOrder ASC, sp.sortOrder ASC, sp.name ASC")
    List<StylePresetEntity> findAvailableForUserWithPreview(@Param("userId") Long userId);

    /**
     * Находит все глобальные пресеты (для админа)
     */
    @Query("SELECT sp FROM StylePresetEntity sp JOIN FETCH sp.category c " +
            "LEFT JOIN FETCH sp.previewImage " +
            "LEFT JOIN FETCH sp.referenceImage WHERE sp.isGlobal = true " +
            "ORDER BY c.sortOrder ASC, sp.sortOrder ASC, sp.name ASC")
    List<StylePresetEntity> findAllGlobal();

    /**
     * Находит все персональные пресеты пользователя
     */
    @Query("SELECT sp FROM StylePresetEntity sp JOIN FETCH sp.category c " +
            "LEFT JOIN FETCH sp.previewImage " +
            "LEFT JOIN FETCH sp.referenceImage WHERE sp.owner.userId = :userId " +
            "ORDER BY c.sortOrder ASC, sp.sortOrder ASC, sp.name ASC")
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
           "(sp.isGlobal = true OR sp.owner.userId = :userId OR sp.publishedToCatalog = true)")
    boolean existsByIdAndAccessibleByUser(Long id, Long userId);

    @Query("SELECT sp FROM StylePresetEntity sp JOIN FETCH sp.category " +
            "LEFT JOIN FETCH sp.previewImage " +
            "LEFT JOIN FETCH sp.referenceImage WHERE sp.id = :id")
    Optional<StylePresetEntity> findByIdWithCategoryAndPreview(@Param("id") Long id);

    @Query("SELECT sp FROM StylePresetEntity sp " +
            "LEFT JOIN FETCH sp.referenceImage WHERE sp.id = :id")
    Optional<StylePresetEntity> findByIdWithReference(@Param("id") Long id);

    /**
     * Персональные пресеты для админ-модерации (с превью, референсом, владельцем).
     */
    @Query("SELECT DISTINCT sp FROM StylePresetEntity sp " +
            "JOIN FETCH sp.category c " +
            "LEFT JOIN FETCH sp.previewImage " +
            "LEFT JOIN FETCH sp.referenceImage " +
            "LEFT JOIN FETCH sp.owner " +
            "WHERE sp.isGlobal = false " +
            "AND (:status IS NULL OR sp.moderationStatus = :status) " +
            "ORDER BY sp.updatedAt DESC")
    List<StylePresetEntity> findUserPresetsForAdmin(@Param("status") PresetModerationStatus status);

    long countByIsGlobalFalse();

    @Query("SELECT COUNT(sp) FROM StylePresetEntity sp " +
            "WHERE sp.isGlobal = false AND sp.referenceImage IS NOT NULL")
    long countUserPresetsWithReferenceImage();

    @Query("SELECT sp.moderationStatus, COUNT(sp) FROM StylePresetEntity sp " +
            "WHERE sp.isGlobal = false GROUP BY sp.moderationStatus")
    List<Object[]> countUserPresetsGroupedByModerationStatus();

    List<StylePresetEntity> findByCategory_Id(Long categoryId);
}

package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.PromptEnhancerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptEnhancerRepository extends JpaRepository<PromptEnhancerEntity, Long> {

    /**
     * Находит все доступные энхансеры для пользователя:
     * - глобальные энхансеры (is_global = true)
     * - персональные энхансеры пользователя (owner_id = userId)
     * Только активные (is_enabled = true)
     * Сортировка по sort_order для правильного порядка применения
     */
    @Query("SELECT pe FROM PromptEnhancerEntity pe WHERE " +
           "(pe.isGlobal = true OR pe.owner.userId = :userId) AND pe.isEnabled = true " +
           "ORDER BY pe.sortOrder ASC, pe.name ASC")
    List<PromptEnhancerEntity> findAvailableForUser(Long userId);

    /**
     * Находит все глобальные энхансеры (для админа)
     */
    @Query("SELECT pe FROM PromptEnhancerEntity pe WHERE pe.isGlobal = true ORDER BY pe.sortOrder ASC, pe.name ASC")
    List<PromptEnhancerEntity> findAllGlobal();

    /**
     * Находит все персональные энхансеры пользователя
     */
    @Query("SELECT pe FROM PromptEnhancerEntity pe WHERE pe.owner.userId = :userId ORDER BY pe.sortOrder ASC, pe.name ASC")
    List<PromptEnhancerEntity> findByOwnerUserId(Long userId);

    /**
     * Находит энхансер по коду и владельцу (для проверки уникальности)
     */
    Optional<PromptEnhancerEntity> findByCodeAndOwner_UserId(String code, Long ownerId);

    /**
     * Находит глобальный энхансер по коду
     */
    Optional<PromptEnhancerEntity> findByCodeAndIsGlobalTrue(String code);

    /**
     * Проверяет существование энхансера по ID и владельцу
     */
    @Query("SELECT COUNT(pe) > 0 FROM PromptEnhancerEntity pe WHERE pe.id = :id AND " +
           "(pe.isGlobal = true OR pe.owner.userId = :userId)")
    boolean existsByIdAndAccessibleByUser(Long id, Long userId);
}

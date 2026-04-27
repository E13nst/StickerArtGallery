package com.example.sticker_art_gallery.repository.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.UserPresetLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPresetLikeRepository extends JpaRepository<UserPresetLikeEntity, Long> {

    boolean existsByUserIdAndPresetId(Long userId, Long presetId);

    Optional<UserPresetLikeEntity> findByUserIdAndPresetId(Long userId, Long presetId);

    void deleteByUserIdAndPresetId(Long userId, Long presetId);

    /**
     * Все пресеты, лайкнутые пользователем (для виртуальной категории).
     */
    @Query("SELECT upl.preset FROM UserPresetLikeEntity upl " +
           "WHERE upl.userId = :userId " +
           "ORDER BY upl.createdAt DESC")
    List<StylePresetEntity> findLikedPresetsByUserId(@Param("userId") Long userId);
}

package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.StylePresetCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StylePresetCategoryRepository extends JpaRepository<StylePresetCategoryEntity, Long> {

    Optional<StylePresetCategoryEntity> findByCode(String code);
}

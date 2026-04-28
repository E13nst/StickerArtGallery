package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.generation.UserPresetCreationBlueprintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPresetCreationBlueprintRepository extends JpaRepository<UserPresetCreationBlueprintEntity, Long> {

    Optional<UserPresetCreationBlueprintEntity> findByCode(String code);

    List<UserPresetCreationBlueprintEntity> findByEnabledTrueOrderBySortOrderAscIdAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}

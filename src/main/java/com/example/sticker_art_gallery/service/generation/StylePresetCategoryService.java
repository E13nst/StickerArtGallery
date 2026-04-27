package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetCategoryRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetCategoryDto;
import com.example.sticker_art_gallery.dto.generation.UpdateStylePresetCategoryRequest;
import com.example.sticker_art_gallery.model.generation.StylePresetCategoryEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.repository.StylePresetCategoryRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StylePresetCategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetCategoryService.class);

    private static final String GENERAL_CODE = "general";

    private final StylePresetCategoryRepository categoryRepository;
    private final StylePresetRepository presetRepository;

    public StylePresetCategoryService(
            StylePresetCategoryRepository categoryRepository,
            StylePresetRepository presetRepository) {
        this.categoryRepository = categoryRepository;
        this.presetRepository = presetRepository;
    }

    @Transactional(readOnly = true)
    public List<StylePresetCategoryDto> listAllOrdered() {
        return categoryRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StylePresetCategoryDto create(CreateStylePresetCategoryRequest request) {
        String code = request.getCode().trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Category code cannot be empty");
        }
        categoryRepository.findByCode(code).ifPresent(c -> {
            throw new IllegalArgumentException("Category with code '" + code + "' already exists");
        });
        StylePresetCategoryEntity e = new StylePresetCategoryEntity();
        e.setCode(code);
        e.setName(request.getName().trim());
        e.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        e = categoryRepository.save(e);
        LOGGER.info("Created style preset category id={}, code={}", e.getId(), e.getCode());
        return toDto(e);
    }

    @Transactional
    public StylePresetCategoryDto update(Long id, UpdateStylePresetCategoryRequest request) {
        StylePresetCategoryEntity e = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        e.setName(request.getName().trim());
        e.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        e = categoryRepository.save(e);
        LOGGER.info("Updated style preset category id={}", id);
        return toDto(e);
    }

    @Transactional
    public void delete(Long id) {
        StylePresetCategoryEntity general = categoryRepository.findByCode(GENERAL_CODE)
                .orElseThrow(() -> new IllegalStateException("Default category '" + GENERAL_CODE + "' not found"));
        if (general.getId().equals(id)) {
            throw new IllegalArgumentException("Cannot delete the default category");
        }
        StylePresetCategoryEntity toDelete = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        List<StylePresetEntity> presets = presetRepository.findByCategory_Id(toDelete.getId());
        for (StylePresetEntity p : presets) {
            p.setCategory(general);
        }
        presetRepository.saveAll(presets);
        categoryRepository.delete(toDelete);
        LOGGER.info("Deleted style preset category id={}, reassigned {} presets to general", id, presets.size());
    }

    public StylePresetCategoryDto toDto(StylePresetCategoryEntity e) {
        if (e == null) {
            return null;
        }
        StylePresetCategoryDto d = new StylePresetCategoryDto();
        d.setId(e.getId());
        d.setCode(e.getCode());
        d.setName(e.getName());
        d.setSortOrder(e.getSortOrder());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}

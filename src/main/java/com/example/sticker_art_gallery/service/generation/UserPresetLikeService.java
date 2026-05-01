package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.UserPresetLikeEntity;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.UserPresetLikeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис «Сохранённых пресетов» пользователя (виртуальная категория «Лайкнутые пресеты»).
 */
@Service
@Transactional
public class UserPresetLikeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPresetLikeService.class);

    private final UserPresetLikeRepository userPresetLikeRepository;
    private final StylePresetRepository presetRepository;
    private final StylePresetService stylePresetService;

    public UserPresetLikeService(UserPresetLikeRepository userPresetLikeRepository,
                                  StylePresetRepository presetRepository,
                                  StylePresetService stylePresetService) {
        this.userPresetLikeRepository = userPresetLikeRepository;
        this.presetRepository = presetRepository;
        this.stylePresetService = stylePresetService;
    }

    /**
     * Добавить пресет в «Сохранённые» пользователя.
     */
    public void likePreset(Long userId, Long presetId) {
        if (userPresetLikeRepository.existsByUserIdAndPresetId(userId, presetId)) {
            throw new IllegalArgumentException("Пресет уже добавлен в сохранённые");
        }
        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));

        UserPresetLikeEntity like = new UserPresetLikeEntity();
        like.setUserId(userId);
        like.setPreset(preset);
        userPresetLikeRepository.save(like);
        LOGGER.info("Пользователь {} добавил пресет {} в сохранённые", userId, presetId);
    }

    /**
     * Убрать пресет из «Сохранённых» пользователя.
     */
    public void unlikePreset(Long userId, Long presetId) {
        if (!userPresetLikeRepository.existsByUserIdAndPresetId(userId, presetId)) {
            throw new IllegalArgumentException("Пресет не найден в сохранённых");
        }
        userPresetLikeRepository.deleteByUserIdAndPresetId(userId, presetId);
        LOGGER.info("Пользователь {} убрал пресет {} из сохранённых", userId, presetId);
    }

    /**
     * Получить список сохранённых пресетов пользователя.
     */
    @Transactional(readOnly = true)
    public List<StylePresetDto> getLikedPresets(Long userId, boolean suppressConsumerPrivacy) {
        Long viewer = suppressConsumerPrivacy ? null : userId;
        return userPresetLikeRepository.findLikedPresetsByUserId(userId).stream()
                .map(p -> stylePresetService.toDto(p, true, viewer))
                .collect(Collectors.toList());
    }

    /**
     * Проверить, сохранён ли пресет пользователем.
     */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long userId, Long presetId) {
        return userPresetLikeRepository.existsByUserIdAndPresetId(userId, presetId);
    }
}

package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreatePromptEnhancerRequest;
import com.example.sticker_art_gallery.dto.generation.PromptEnhancerDto;
import com.example.sticker_art_gallery.model.generation.PromptEnhancerEntity;
import com.example.sticker_art_gallery.repository.PromptEnhancerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptEnhancerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptEnhancerService.class);

    private final PromptEnhancerRepository enhancerRepository;

    @Autowired
    public PromptEnhancerService(PromptEnhancerRepository enhancerRepository) {
        this.enhancerRepository = enhancerRepository;
    }

    /**
     * Получает все глобальные энхансеры (для админа)
     */
    @Transactional(readOnly = true)
    public List<PromptEnhancerDto> getAllGlobalEnhancers() {
        LOGGER.info("Getting all global enhancers");
        List<PromptEnhancerEntity> enhancers = enhancerRepository.findAllGlobal();
        return enhancers.stream()
                .map(PromptEnhancerDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Создает глобальный энхансер (только для админа)
     */
    @Transactional
    public PromptEnhancerDto createGlobalEnhancer(CreatePromptEnhancerRequest request) {
        LOGGER.info("Creating global enhancer: code={}", request.getCode());

        // Проверяем уникальность кода для глобальных энхансеров
        enhancerRepository.findByCodeAndIsGlobalTrue(request.getCode())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Global enhancer with code '" + request.getCode() + "' already exists");
                });

        PromptEnhancerEntity enhancer = new PromptEnhancerEntity();
        enhancer.setCode(request.getCode());
        enhancer.setName(request.getName());
        enhancer.setDescription(request.getDescription());
        enhancer.setSystemPrompt(request.getSystemPrompt());
        enhancer.setIsGlobal(true);
        enhancer.setOwner(null);
        enhancer.setIsEnabled(true);
        enhancer.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        enhancer = enhancerRepository.save(enhancer);
        LOGGER.info("Created global enhancer: id={}, code={}", enhancer.getId(), enhancer.getCode());
        return PromptEnhancerDto.fromEntity(enhancer);
    }

    /**
     * Обновляет энхансер (только для админа)
     */
    @Transactional
    public PromptEnhancerDto updateEnhancer(Long enhancerId, CreatePromptEnhancerRequest request) {
        LOGGER.info("Updating enhancer: id={}", enhancerId);

        PromptEnhancerEntity enhancer = enhancerRepository.findById(enhancerId)
                .orElseThrow(() -> new IllegalArgumentException("Enhancer not found: " + enhancerId));

        // Обновляем поля (код не меняем)
        enhancer.setName(request.getName());
        enhancer.setDescription(request.getDescription());
        enhancer.setSystemPrompt(request.getSystemPrompt());
        if (request.getSortOrder() != null) {
            enhancer.setSortOrder(request.getSortOrder());
        }

        enhancer = enhancerRepository.save(enhancer);
        LOGGER.info("Updated enhancer: id={}", enhancerId);
        return PromptEnhancerDto.fromEntity(enhancer);
    }

    /**
     * Удаляет энхансер (только для админа)
     */
    @Transactional
    public void deleteEnhancer(Long enhancerId) {
        LOGGER.info("Deleting enhancer: id={}", enhancerId);

        PromptEnhancerEntity enhancer = enhancerRepository.findById(enhancerId)
                .orElseThrow(() -> new IllegalArgumentException("Enhancer not found: " + enhancerId));

        enhancerRepository.delete(enhancer);
        LOGGER.info("Deleted enhancer: id={}", enhancerId);
    }

    /**
     * Включает/выключает энхансер (только для админа)
     */
    @Transactional
    public PromptEnhancerDto toggleEnhancerEnabled(Long enhancerId, boolean enabled) {
        LOGGER.info("Toggling enhancer enabled: id={}, enabled={}", enhancerId, enabled);

        PromptEnhancerEntity enhancer = enhancerRepository.findById(enhancerId)
                .orElseThrow(() -> new IllegalArgumentException("Enhancer not found: " + enhancerId));

        enhancer.setIsEnabled(enabled);
        enhancer = enhancerRepository.save(enhancer);
        LOGGER.info("Toggled enhancer enabled: id={}, enabled={}", enhancerId, enabled);
        return PromptEnhancerDto.fromEntity(enhancer);
    }
}

package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import com.example.sticker_art_gallery.repository.ArtRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ArtRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtRuleService.class);

    private final ArtRuleRepository artRuleRepository;

    public ArtRuleService(ArtRuleRepository artRuleRepository) {
        this.artRuleRepository = artRuleRepository;
    }

    @Cacheable(cacheNames = "artRules", key = "#code")
    @Transactional(readOnly = true)
    public ArtRuleEntity getEnabledRuleOrThrow(String code) {
        return artRuleRepository.findByCode(code)
                .filter(ArtRuleEntity::getIsEnabled)
                .orElseThrow(() -> {
                    LOGGER.warn("⚠️ Правило ART с кодом {} не найдено или отключено", code);
                    return new IllegalArgumentException("Правило ART не найдено или отключено: " + code);
                });
    }

    @Transactional(readOnly = true)
    public List<ArtRuleEntity> getAllRules() {
        return artRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ArtRuleEntity> findByCode(String code) {
        return artRuleRepository.findByCode(code);
    }

    @CacheEvict(cacheNames = "artRules", key = "#result.code", condition = "#result != null")
    public ArtRuleEntity save(ArtRuleEntity rule) {
        ArtRuleEntity saved = artRuleRepository.save(rule);
        LOGGER.info("✅ Правило ART сохранено: code={}, amount={}, enabled={}",
                saved.getCode(), saved.getAmount(), saved.getIsEnabled());
        return saved;
    }

    @CacheEvict(cacheNames = "artRules", allEntries = true)
    public void invalidateCache() {
        LOGGER.debug("♻️ Кэш правил ART очищен");
    }
}


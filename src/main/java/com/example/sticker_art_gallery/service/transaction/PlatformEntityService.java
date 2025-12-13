package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.model.transaction.EntityType;
import com.example.sticker_art_gallery.model.transaction.PlatformEntityEntity;
import com.example.sticker_art_gallery.model.transaction.PlatformEntityRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Сервис для работы с универсальными сущностями
 */
@Service
@Transactional
public class PlatformEntityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformEntityService.class);

    private final PlatformEntityRepository repository;
    private final UserRepository userRepository;

    public PlatformEntityService(PlatformEntityRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /**
     * Получить или создать сущность
     */
    public PlatformEntityEntity getOrCreate(EntityType type, String entityRef, Long ownerUserId) {
        Optional<PlatformEntityEntity> existing = repository.findByEntityTypeAndEntityRef(type, entityRef);
        
        if (existing.isPresent()) {
            LOGGER.debug("Найдена существующая сущность: type={}, ref={}", type, entityRef);
            return existing.get();
        }

        LOGGER.info("Создание новой сущности: type={}, ref={}, ownerUserId={}", type, entityRef, ownerUserId);
        
        PlatformEntityEntity entity = new PlatformEntityEntity();
        entity.setEntityType(type);
        entity.setEntityRef(entityRef);
        
        if (ownerUserId != null) {
            Optional<UserEntity> ownerOpt = userRepository.findById(ownerUserId);
            if (ownerOpt.isPresent()) {
                entity.setOwnerUser(ownerOpt.get());
            } else {
                LOGGER.warn("Владелец с userId={} не найден, создаем сущность без владельца", ownerUserId);
            }
        }
        
        return repository.save(entity);
    }

    /**
     * Найти сущность по типу и ссылке
     */
    @Transactional(readOnly = true)
    public Optional<PlatformEntityEntity> findByTypeAndRef(EntityType type, String entityRef) {
        return repository.findByEntityTypeAndEntityRef(type, entityRef);
    }
}


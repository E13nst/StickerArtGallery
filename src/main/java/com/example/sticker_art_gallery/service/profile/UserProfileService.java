package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.dto.UpdateUserProfileRequest;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.UserProfileRepository;
import com.example.sticker_art_gallery.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository repository;
    private final UserService userService;

    @Autowired
    public UserProfileService(UserProfileRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public Optional<UserProfileEntity> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<UserProfileEntity> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public Optional<UserProfileEntity> findByTelegramId(Long telegramId) {
        return repository.findByUserId(telegramId);
    }

    public UserProfileEntity getOrCreateDefault(Long userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            UserProfileEntity profile = new UserProfileEntity();
            profile.setUserId(userId);
            profile.setRole(UserProfileEntity.UserRole.USER);
            profile.setArtBalance(0L);
            profile.setIsBlocked(false);
            return repository.save(profile);
        });
    }

    public UserProfileEntity getOrCreateDefaultForUpdate(Long userId) {
        return repository.findByUserIdForUpdate(userId).orElseGet(() -> {
            UserProfileEntity profile = new UserProfileEntity();
            profile.setUserId(userId);
            profile.setRole(UserProfileEntity.UserRole.USER);
            profile.setArtBalance(0L);
            profile.setIsBlocked(false);
            return repository.save(profile);
        });
    }

    public UserProfileEntity save(UserProfileEntity profile) {
        return repository.save(profile);
    }

    /**
     * Атомарно создает или обновляет пользователя и его профиль в одной транзакции.
     * Гарантирует целостность данных: если создание профиля не удастся, 
     * изменения в таблице users также будут откачены.
     * 
     * @param telegramId Telegram ID пользователя
     * @param firstName Имя пользователя
     * @param lastName Фамилия пользователя
     * @param username Username пользователя
     * @param languageCode Код языка пользователя
     * @param isPremium Статус Telegram Premium
     * @return Созданный или обновленный профиль пользователя
     */
    public UserProfileEntity ensureUserAndProfileExists(
            Long telegramId,
            String firstName,
            String lastName,
            String username,
            String languageCode,
            Boolean isPremium) {
        // Создаем/обновляем пользователя в users
        userService.upsertFromTelegramData(telegramId, firstName, lastName, username, languageCode, isPremium);
        
        // Создаем/обновляем профиль в user_profiles с блокировкой
        return getOrCreateDefaultForUpdate(telegramId);
    }

    // ============ Admin methods ============

    /**
     * Получить список всех профилей с фильтрами и пагинацией (для админ-панели)
     * 
     * @param role Фильтр по роли (USER/ADMIN)
     * @param isBlocked Фильтр по статусу блокировки
     * @param subscriptionStatus Фильтр по статусу подписки
     * @param minBalance Минимальный баланс
     * @param maxBalance Максимальный баланс
     * @param createdAfter Дата создания после
     * @param createdBefore Дата создания до
     * @param search Поиск по User ID
     * @param pageable Параметры пагинации и сортировки
     * @return Страница профилей пользователей
     */
    @Transactional(readOnly = true)
    public Page<UserProfileEntity> findAllWithFilters(
            UserProfileEntity.UserRole role,
            Boolean isBlocked,
            UserProfileEntity.SubscriptionStatus subscriptionStatus,
            Long minBalance,
            Long maxBalance,
            OffsetDateTime createdAfter,
            OffsetDateTime createdBefore,
            String search,
            Pageable pageable) {
        LOGGER.debug("🔍 Поиск профилей с фильтрами: role={}, isBlocked={}, subscriptionStatus={}, " +
                     "minBalance={}, maxBalance={}, search={}, page={}, size={}",
                     role, isBlocked, subscriptionStatus, minBalance, maxBalance, search,
                     pageable.getPageNumber(), pageable.getPageSize());
        
        // Преобразуем enum в строки для нативного SQL запроса
        String roleStr = role != null ? role.name() : null;
        String subscriptionStatusStr = subscriptionStatus != null ? subscriptionStatus.name() : null;
        String createdAfterStr = createdAfter != null ? createdAfter.toString() : null;
        String createdBeforeStr = createdBefore != null ? createdBefore.toString() : null;
        
        return repository.findAllWithFilters(
                roleStr, isBlocked, subscriptionStatusStr,
                minBalance, maxBalance,
                createdAfterStr, createdBeforeStr,
                search, pageable
        );
    }

    /**
     * Обновить профиль пользователя (только для админа)
     * 
     * @param userId Telegram ID пользователя
     * @param request Данные для обновления
     * @return Обновленный профиль
     * @throws IllegalArgumentException если профиль не найден
     */
    @Transactional
    public UserProfileEntity updateProfile(Long userId, UpdateUserProfileRequest request) {
        LOGGER.info("✏️ Обновление профиля пользователя {}: role={}, artBalance={}, isBlocked={}, subscriptionStatus={}",
                    userId, request.getRole(), request.getArtBalance(), request.getIsBlocked(), request.getSubscriptionStatus());
        
        UserProfileEntity profile = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Профиль пользователя с ID " + userId + " не найден"));
        
        // Обновляем только те поля, которые переданы в запросе (не null)
        if (request.getRole() != null) {
            profile.setRole(request.getRole());
            LOGGER.debug("  ✓ Роль изменена на: {}", request.getRole());
        }
        
        if (request.getArtBalance() != null) {
            profile.setArtBalance(request.getArtBalance());
            LOGGER.debug("  ✓ Баланс изменен на: {}", request.getArtBalance());
        }
        
        if (request.getIsBlocked() != null) {
            profile.setIsBlocked(request.getIsBlocked());
            LOGGER.debug("  ✓ Статус блокировки изменен на: {}", request.getIsBlocked());
        }
        
        if (request.getSubscriptionStatus() != null) {
            profile.setSubscriptionStatus(request.getSubscriptionStatus());
            LOGGER.debug("  ✓ Статус подписки изменен на: {}", request.getSubscriptionStatus());
        }
        
        UserProfileEntity savedProfile = repository.save(profile);
        LOGGER.info("✅ Профиль пользователя {} успешно обновлен", userId);
        
        return savedProfile;
    }
}



package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.UserProfileRepository;
import com.example.sticker_art_gallery.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserProfileService {

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
}



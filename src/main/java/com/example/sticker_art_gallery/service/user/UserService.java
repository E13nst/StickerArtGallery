package com.example.sticker_art_gallery.service.user;

import com.example.sticker_art_gallery.dto.UserDto;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с пользователями (кэш данных из Telegram)
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final TelegramBotApiService telegramBotApiService;
    
    @Autowired
    public UserService(UserRepository userRepository, TelegramBotApiService telegramBotApiService) {
        this.userRepository = userRepository;
        this.telegramBotApiService = telegramBotApiService;
    }
    
    /**
     * Найти или создать пользователя и обновить его данные из Telegram
     * Вызывается при каждой аутентификации
     */
    public UserEntity upsertFromTelegramData(Long telegramId, String firstName, String lastName, 
                                            String username, String languageCode, Boolean isPremium) {
        LOGGER.debug("🔍 Поиск или создание пользователя с telegram_id: {}", telegramId);
        
        Optional<UserEntity> existingUser = userRepository.findById(telegramId);
        
        UserEntity user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            LOGGER.debug("✅ Пользователь найден: {}, обновляем данные", user.getUsername());
        } else {
            user = new UserEntity(telegramId);
            LOGGER.debug("🆕 Создаем нового пользователя с telegram_id: {}", telegramId);
        }
        
        // Обновляем данные из Telegram
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setLanguageCode(languageCode);
        user.setIsPremium(isPremium);
        
        user = userRepository.save(user);
        LOGGER.info("✅ Пользователь сохранен/обновлен: id={}, username={}", user.getId(), user.getUsername());
        
        return user;
    }
    
    /**
     * Найти пользователя по ID
     */
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Найти пользователя по username
     */
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Получить всех пользователей
     */
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }
    
    /**
     * Получить фото профиля пользователя из Bot API
     * Возвращает объект с информацией о фото и file_id самого большого фото
     */
    public Map<String, Object> getUserProfilePhoto(Long userId) {
        try {
            Object profilePhotosData = telegramBotApiService.getUserProfilePhotos(userId);
            
            if (profilePhotosData == null) {
                return null;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("profilePhotos", profilePhotosData);
            
            // Извлекаем file_id самого большого фото
            String largestPhotoFileId = extractLargestPhotoFileId(profilePhotosData);
            result.put("profilePhotoFileId", largestPhotoFileId);
            
            return result;
            
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось получить фото профиля для пользователя '{}': {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Извлекает file_id самого большого фото из profilePhotos
     */
    @SuppressWarnings("unchecked")
    private String extractLargestPhotoFileId(Object profilePhotosObj) {
        try {
            if (profilePhotosObj == null) {
                return null;
            }
            
            Map<String, Object> profilePhotos = (Map<String, Object>) profilePhotosObj;
            
            Object totalCountObj = profilePhotos.get("total_count");
            if (totalCountObj == null || ((Number) totalCountObj).intValue() == 0) {
                return null;
            }
            
            List<List<Map<String, Object>>> photos = (List<List<Map<String, Object>>>) profilePhotos.get("photos");
            if (photos == null || photos.isEmpty() || photos.get(0).isEmpty()) {
                return null;
            }
            
            List<Map<String, Object>> photoSizes = photos.get(0);
            
            Map<String, Object> largestPhoto = photoSizes.stream()
                    .max((p1, p2) -> {
                        int size1 = p1.get("file_size") != null ? ((Number) p1.get("file_size")).intValue() : 0;
                        int size2 = p2.get("file_size") != null ? ((Number) p2.get("file_size")).intValue() : 0;
                        return Integer.compare(size1, size2);
                    })
                    .orElse(null);
            
            if (largestPhoto != null && largestPhoto.get("file_id") != null) {
                return (String) largestPhoto.get("file_id");
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при извлечении file_id фото профиля: {}", e.getMessage());
            return null;
        }
    }
}


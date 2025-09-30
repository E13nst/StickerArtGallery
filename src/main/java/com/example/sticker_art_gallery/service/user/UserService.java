package com.example.sticker_art_gallery.service.user;

import com.example.sticker_art_gallery.dto.UserDto;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с пользователями
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final TelegramBotApiService telegramBotApiService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public UserService(UserRepository userRepository, TelegramBotApiService telegramBotApiService, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.telegramBotApiService = telegramBotApiService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Найти или создать пользователя по telegram_id
     * 
     * @param telegramId ID пользователя в Telegram
     * @param username username пользователя (может быть null)
     * @param firstName имя пользователя (может быть null)
     * @param lastName фамилия пользователя (может быть null)
     * @param avatarUrl URL аватара (может быть null)
     * @return найденный или созданный пользователь
     */
    public UserEntity findOrCreateByTelegramId(Long telegramId, String username, String firstName, 
                                             String lastName, String avatarUrl) {
        LOGGER.info("🔍 Поиск пользователя по telegram_id: {}", telegramId);
        
        Optional<UserEntity> existingUser = userRepository.findByTelegramId(telegramId);
        
        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            LOGGER.info("✅ Пользователь найден: {}", user.getUsername());
            
            // Обновляем данные пользователя, если они изменились
            boolean updated = false;
            if (username != null && !username.equals(user.getUsername())) {
                user.setUsername(username);
                updated = true;
            }
            if (firstName != null && !firstName.equals(user.getFirstName())) {
                user.setFirstName(firstName);
                updated = true;
            }
            if (lastName != null && !lastName.equals(user.getLastName())) {
                user.setLastName(lastName);
                updated = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                updated = true;
            }
            
            if (updated) {
                user = userRepository.save(user);
                LOGGER.info("🔄 Данные пользователя обновлены: {}", user.getUsername());
            }
            
            return user;
        } else {
            LOGGER.info("🆕 Создание нового пользователя с telegram_id: {}", telegramId);
            
            UserEntity newUser = new UserEntity(telegramId, username, firstName, lastName, avatarUrl);
            newUser = userRepository.save(newUser);
            
            LOGGER.info("✅ Новый пользователь создан: {} (ID: {})", newUser.getUsername(), newUser.getId());
            return newUser;
        }
    }
    
    /**
     * Найти пользователя по telegram_id
     */
    public Optional<UserEntity> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
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
     * Получить всех пользователей как DTO
     */
    public List<UserDto> findAllAsDto() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Сохранить пользователя
     */
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }
    
    /**
     * Удалить пользователя по ID
     */
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
    
    /**
     * Проверить существование пользователя по telegram_id
     */
    public boolean existsByTelegramId(Long telegramId) {
        return userRepository.existsByTelegramId(telegramId);
    }
    
    /**
     * Найти пользователей по роли
     */
    public List<UserEntity> findByRole(UserEntity.UserRole role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * Обновить баланс пользователя
     */
    public UserEntity updateArtBalance(Long userId, Long newBalance) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setArtBalance(newBalance);
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
    }
    
    /**
     * Добавить к балансу пользователя
     */
    public UserEntity addToArtBalance(Long userId, Long amount) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setArtBalance(user.getArtBalance() + amount);
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
    }
    
    /**
     * Обогащает одного пользователя данными из Bot API (безопасно)
     * Если данные Bot API недоступны, возвращает DTO без обогащения, но не выбрасывает исключение
     */
    public UserDto enrichSingleUserSafely(UserEntity user) {
        UserDto dto = UserDto.fromEntity(user);
        
        // Обогащаем данными getChatMember
        try {
            Object botApiData = telegramBotApiService.getUserInfo(user.getId());
            dto.setTelegramUserInfo(botApiData);
            LOGGER.debug("✅ Пользователь '{}' обогащен данными Bot API (getChatMember)", user.getId());
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось получить данные Bot API для пользователя '{}': {} - пропускаем обогащение", 
                    user.getId(), e.getMessage());
            dto.setTelegramUserInfo(null);
        }
        
        // Обогащаем данными getUserProfilePhotos
        try {
            Object profilePhotosData = telegramBotApiService.getUserProfilePhotos(user.getId());
            dto.setProfilePhotos(profilePhotosData);
            
            // Извлекаем file_id самого большого фото
            String largestPhotoFileId = extractLargestPhotoFileId(profilePhotosData);
            dto.setProfilePhotoFileId(largestPhotoFileId);
            
            if (largestPhotoFileId != null) {
                LOGGER.debug("✅ Пользователь '{}' обогащен фото профиля: file_id={}", user.getId(), largestPhotoFileId);
            } else {
                LOGGER.debug("📷 У пользователя '{}' нет фото профиля", user.getId());
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось получить фото профиля для пользователя '{}': {} - пропускаем", 
                    user.getId(), e.getMessage());
            dto.setProfilePhotos(null);
            dto.setProfilePhotoFileId(null);
        }
        
        return dto;
    }
    
    /**
     * Обогащает список пользователей данными из Bot API (безопасно)
     */
    public List<UserDto> enrichUsersSafely(List<UserEntity> users) {
        return users.stream()
                .map(this::enrichSingleUserSafely)
                .collect(Collectors.toList());
    }
    
    /**
     * Извлекает file_id самого большого фото из profilePhotos
     * Возвращает null если фото нет или произошла ошибка
     */
    @SuppressWarnings("unchecked")
    private String extractLargestPhotoFileId(Object profilePhotosObj) {
        try {
            if (profilePhotosObj == null) {
                return null;
            }
            
            Map<String, Object> profilePhotos = (Map<String, Object>) profilePhotosObj;
            
            // Проверяем total_count
            Object totalCountObj = profilePhotos.get("total_count");
            if (totalCountObj == null || ((Number) totalCountObj).intValue() == 0) {
                LOGGER.debug("📷 У пользователя нет фото профиля");
                return null;
            }
            
            // Получаем массив photos
            List<List<Map<String, Object>>> photos = (List<List<Map<String, Object>>>) profilePhotos.get("photos");
            if (photos == null || photos.isEmpty() || photos.get(0).isEmpty()) {
                LOGGER.debug("📷 Массив фото пуст");
                return null;
            }
            
            // Берем первый массив размеров (это последнее фото пользователя)
            List<Map<String, Object>> photoSizes = photos.get(0);
            
            // Ищем самое большое фото по file_size
            Map<String, Object> largestPhoto = photoSizes.stream()
                    .max((p1, p2) -> {
                        int size1 = p1.get("file_size") != null ? ((Number) p1.get("file_size")).intValue() : 0;
                        int size2 = p2.get("file_size") != null ? ((Number) p2.get("file_size")).intValue() : 0;
                        return Integer.compare(size1, size2);
                    })
                    .orElse(null);
            
            if (largestPhoto != null && largestPhoto.get("file_id") != null) {
                String fileId = (String) largestPhoto.get("file_id");
                LOGGER.debug("📷 Найдено самое большое фото: file_id={}, size={}", 
                        fileId, largestPhoto.get("file_size"));
                return fileId;
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при извлечении file_id фото профиля: {} - возвращаем null", e.getMessage());
            return null;
        }
    }
}

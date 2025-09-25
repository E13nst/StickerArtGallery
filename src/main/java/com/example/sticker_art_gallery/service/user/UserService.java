package com.example.sticker_art_gallery.service.user;

import com.example.sticker_art_gallery.dto.UserDto;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}

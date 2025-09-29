package com.example.sticker_art_gallery.model.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * Найти пользователя по telegram_id (теперь это id)
     */
    default Optional<UserEntity> findByTelegramId(Long telegramId) {
        return findById(telegramId);
    }
    
    /**
     * Найти пользователя по username
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Проверить существование пользователя по telegram_id (теперь это id)
     */
    default boolean existsByTelegramId(Long telegramId) {
        return existsById(telegramId);
    }
    
    /**
     * Проверить существование пользователя по username
     */
    boolean existsByUsername(String username);
    
    /**
     * Найти всех пользователей с определенной ролью
     */
    @Query("SELECT u FROM UserEntity u WHERE u.role = :role")
    java.util.List<UserEntity> findByRole(@Param("role") UserEntity.UserRole role);
    
    /**
     * Найти пользователей с балансом больше указанного
     */
    @Query("SELECT u FROM UserEntity u WHERE u.artBalance > :minBalance")
    java.util.List<UserEntity> findByArtBalanceGreaterThan(@Param("minBalance") Long minBalance);
}

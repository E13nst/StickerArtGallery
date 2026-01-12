package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository для работы с пользователями (кэш данных из Telegram)
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * Найти пользователя по username
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Проверить существование пользователя по username
     */
    boolean existsByUsername(String username);

    long countByCreatedAtAfter(OffsetDateTime createdAfter);

    /**
     * Подсчитать количество пользователей за период между двумя датами
     */
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}

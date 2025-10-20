package com.example.sticker_art_gallery.model.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
    
    /**
     * Найти профиль по userId (Telegram ID)
     */
    Optional<UserProfileEntity> findByUserId(Long userId);
    
    /**
     * Проверить существование профиля по userId
     */
    boolean existsByUserId(Long userId);
}



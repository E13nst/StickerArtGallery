package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    /**
     * Найти профиль по userId (Telegram ID)
     */
    Optional<UserProfileEntity> findByUserId(Long userId);

    /**
     * Найти профиль по userId с блокировкой для обновления
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserProfileEntity up where up.userId = :userId")
    Optional<UserProfileEntity> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Проверить существование профиля по userId
     */
    boolean existsByUserId(Long userId);
}

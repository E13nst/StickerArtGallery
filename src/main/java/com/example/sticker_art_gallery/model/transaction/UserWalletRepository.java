package com.example.sticker_art_gallery.model.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с кошельками пользователей
 */
@Repository
public interface UserWalletRepository extends JpaRepository<UserWalletEntity, Long> {

    /**
     * Найти все активные кошельки пользователя
     */
    List<UserWalletEntity> findByUser_IdAndIsActiveTrue(Long userId);

    /**
     * Найти кошелек по адресу и пользователю
     */
    Optional<UserWalletEntity> findByUser_IdAndWalletAddress(Long userId, String walletAddress);

    /**
     * Проверить существование кошелька у пользователя
     */
    boolean existsByUser_IdAndWalletAddress(Long userId, String walletAddress);
}


package com.example.sticker_art_gallery.repository.transaction;

import com.example.sticker_art_gallery.model.transaction.IntentStatus;
import com.example.sticker_art_gallery.model.transaction.TransactionIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository для работы с намерениями транзакций
 */
@Repository
public interface TransactionIntentRepository extends JpaRepository<TransactionIntentEntity, Long> {

    /**
     * Найти все намерения пользователя
     */
    List<TransactionIntentEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * Найти намерения по статусу
     */
    List<TransactionIntentEntity> findByStatus(IntentStatus status);

    /**
     * Найти намерения пользователя по статусу
     */
    List<TransactionIntentEntity> findByUser_IdAndStatus(Long userId, IntentStatus status);
}

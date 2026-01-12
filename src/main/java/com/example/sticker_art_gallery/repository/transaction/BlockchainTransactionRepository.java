package com.example.sticker_art_gallery.repository.transaction;

import com.example.sticker_art_gallery.model.transaction.BlockchainTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для работы с транзакциями в блокчейне
 */
@Repository
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransactionEntity, Long> {

    /**
     * Найти транзакцию по хешу
     */
    Optional<BlockchainTransactionEntity> findByTxHash(String txHash);

    /**
     * Найти все транзакции для намерения
     */
    java.util.List<BlockchainTransactionEntity> findByIntent_Id(Long intentId);

    /**
     * Проверить существование транзакции по хешу
     */
    boolean existsByTxHash(String txHash);
}

package com.example.sticker_art_gallery.model.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository для работы с частями транзакций
 */
@Repository
public interface TransactionLegRepository extends JpaRepository<TransactionLegEntity, Long> {

    /**
     * Найти все части транзакции по ID намерения
     */
    List<TransactionLegEntity> findByIntent_Id(Long intentId);

    /**
     * Найти части транзакции по типу
     */
    List<TransactionLegEntity> findByIntent_IdAndLegType(Long intentId, LegType legType);
}


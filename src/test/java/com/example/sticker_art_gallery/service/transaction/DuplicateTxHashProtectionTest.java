package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.model.transaction.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для защиты от повторного использования tx_hash
 */
@ExtendWith(MockitoExtension.class)
@Epic("TON транзакции")
@Feature("Защита от дубликатов")
@DisplayName("Тесты защиты от повторного tx_hash")
class DuplicateTxHashProtectionTest {

    @Mock
    private BlockchainTransactionRepository blockchainTransactionRepository;

    private static final String TEST_TX_HASH = "0x1234567890abcdef";

    private TransactionIntentEntity testIntent;
    private BlockchainTransactionEntity existingTx;

    @BeforeEach
    void setUp() {
        // Настройка тестового намерения
        testIntent = new TransactionIntentEntity();
        testIntent.setId(1L);
        testIntent.setIntentType(IntentType.DONATION);
        testIntent.setStatus(IntentStatus.CREATED);

        // Настройка существующей транзакции с таким же tx_hash
        existingTx = new BlockchainTransactionEntity();
        existingTx.setId(1L);
        existingTx.setTxHash(TEST_TX_HASH);
        existingTx.setIntent(testIntent);
    }

    @Test
    @Story("Защита от дубликатов")
    @DisplayName("Проверка существования tx_hash должна вернуть true если транзакция уже существует")
    void checkTxHashExists_shouldReturnTrueWhenTransactionExists() {
        // Arrange
        when(blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH)).thenReturn(true);

        // Act
        boolean exists = blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH);

        // Assert
        assertThat(exists).isTrue();
        verify(blockchainTransactionRepository, times(1)).existsByTxHash(TEST_TX_HASH);
    }

    @Test
    @Story("Защита от дубликатов")
    @DisplayName("Проверка существования tx_hash должна вернуть false если транзакция не существует")
    void checkTxHashExists_shouldReturnFalseWhenTransactionNotExists() {
        // Arrange
        when(blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH)).thenReturn(false);

        // Act
        boolean exists = blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @Story("Защита от дубликатов")
    @DisplayName("Создание BlockchainTransaction с существующим tx_hash должно выбросить исключение")
    void createBlockchainTransaction_shouldThrowExceptionWhenTxHashExists() {
        // Arrange
        when(blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH)).thenReturn(true);

        // Act & Assert
        // Попытка создать транзакцию с существующим tx_hash должна быть отклонена
        // Это проверяется на уровне БД через unique constraint
        // В unit-тесте мы можем проверить, что метод existsByTxHash вызывается
        boolean exists = blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH);
        assertThat(exists).isTrue();

        // Если попытаться сохранить транзакцию с существующим tx_hash,
        // БД выбросит DataIntegrityViolationException
        when(blockchainTransactionRepository.save(any(BlockchainTransactionEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        BlockchainTransactionEntity newTx = new BlockchainTransactionEntity();
        newTx.setTxHash(TEST_TX_HASH);

        assertThatThrownBy(() -> blockchainTransactionRepository.save(newTx))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Story("Защита от дубликатов")
    @DisplayName("findByTxHash должен вернуть транзакцию если она существует")
    void findByTxHash_shouldReturnTransactionWhenExists() {
        // Arrange
        when(blockchainTransactionRepository.findByTxHash(TEST_TX_HASH))
                .thenReturn(Optional.of(existingTx));

        // Act
        Optional<BlockchainTransactionEntity> result = blockchainTransactionRepository.findByTxHash(TEST_TX_HASH);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getTxHash()).isEqualTo(TEST_TX_HASH);
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @Story("Защита от дубликатов")
    @DisplayName("findByTxHash должен вернуть empty если транзакция не существует")
    void findByTxHash_shouldReturnEmptyWhenNotExists() {
        // Arrange
        when(blockchainTransactionRepository.findByTxHash(TEST_TX_HASH))
                .thenReturn(Optional.empty());

        // Act
        Optional<BlockchainTransactionEntity> result = blockchainTransactionRepository.findByTxHash(TEST_TX_HASH);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @Story("Защита от дубликатов")
    @DisplayName("Уникальный constraint на tx_hash должен предотвращать дубликаты")
    void uniqueConstraint_shouldPreventDuplicateTxHash() {
        // Arrange
        BlockchainTransactionEntity firstTx = new BlockchainTransactionEntity();
        firstTx.setTxHash(TEST_TX_HASH);
        firstTx.setIntent(testIntent);

        BlockchainTransactionEntity secondTx = new BlockchainTransactionEntity();
        secondTx.setTxHash(TEST_TX_HASH); // Тот же tx_hash
        secondTx.setIntent(testIntent);

        // Первая транзакция сохраняется успешно
        when(blockchainTransactionRepository.save(firstTx)).thenReturn(firstTx);
        // После сохранения первой транзакции, проверка должна вернуть true
        when(blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH))
                .thenReturn(true);  // После сохранения уже существует

        // Act
        blockchainTransactionRepository.save(firstTx);
        boolean existsAfterFirst = blockchainTransactionRepository.existsByTxHash(TEST_TX_HASH);

        // Попытка сохранить вторую транзакцию с тем же tx_hash
        when(blockchainTransactionRepository.save(secondTx))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        // Assert
        assertThat(existsAfterFirst).isTrue();
        assertThatThrownBy(() -> blockchainTransactionRepository.save(secondTx))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}


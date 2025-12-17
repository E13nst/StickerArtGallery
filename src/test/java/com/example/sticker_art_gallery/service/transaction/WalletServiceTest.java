package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.exception.WalletNotFoundException;
import com.example.sticker_art_gallery.model.transaction.UserWalletEntity;
import com.example.sticker_art_gallery.model.transaction.UserWalletRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для WalletService
 */
@ExtendWith(MockitoExtension.class)
@Epic("TON транзакции")
@Feature("Сервис кошельков")
@DisplayName("Тесты WalletService")
class WalletServiceTest {

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private static final Long TEST_USER_ID = 123L;
    private static final String TEST_WALLET_ADDRESS = "EQDummyWalletAddress123456789012345678901234";
    private static final String TEST_WALLET_TYPE = "TON";

    private UserEntity testUser;
    private UserWalletEntity testWallet;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(TEST_USER_ID);
        testUser.setUsername("testuser");

        testWallet = new UserWalletEntity();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setWalletAddress(TEST_WALLET_ADDRESS);
        testWallet.setWalletType(TEST_WALLET_TYPE);
        testWallet.setIsActive(true);
        testWallet.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @Story("Привязка кошелька")
    @DisplayName("linkWallet должен создать новый кошелёк и деактивировать старые")
    void linkWallet_shouldCreateNewWalletAndDeactivateOld() {
        // Arrange
        when(walletRepository.findByUser_IdAndWalletAddress(TEST_USER_ID, TEST_WALLET_ADDRESS))
                .thenReturn(Optional.empty());
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        // Старые активные кошельки
        UserWalletEntity oldWallet1 = new UserWalletEntity();
        oldWallet1.setId(2L);
        oldWallet1.setIsActive(true);
        UserWalletEntity oldWallet2 = new UserWalletEntity();
        oldWallet2.setId(3L);
        oldWallet2.setIsActive(true);
        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(oldWallet1, oldWallet2));

        when(walletRepository.save(any(UserWalletEntity.class))).thenAnswer(invocation -> {
            UserWalletEntity wallet = invocation.getArgument(0);
            wallet.setId(1L);
            return wallet;
        });

        // Act
        UserWalletEntity result = walletService.linkWallet(TEST_USER_ID, TEST_WALLET_ADDRESS, TEST_WALLET_TYPE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getWalletAddress()).isEqualTo(TEST_WALLET_ADDRESS);
        assertThat(result.getWalletType()).isEqualTo(TEST_WALLET_TYPE);
        assertThat(result.getIsActive()).isTrue();

        // Проверяем, что старые кошельки были деактивированы
        ArgumentCaptor<UserWalletEntity> walletCaptor = ArgumentCaptor.forClass(UserWalletEntity.class);
        verify(walletRepository, times(3)).save(walletCaptor.capture()); // 2 старых + 1 новый
        
        List<UserWalletEntity> savedWallets = walletCaptor.getAllValues();
        // Проверяем, что старые кошельки были деактивированы
        boolean oldWalletsDeactivated = savedWallets.stream()
                .filter(w -> w.getId() != null && (w.getId().equals(2L) || w.getId().equals(3L)))
                .allMatch(w -> !w.getIsActive());
        assertThat(oldWalletsDeactivated).isTrue();
    }

    @Test
    @Story("Привязка кошелька")
    @DisplayName("linkWallet должен активировать существующий кошелёк и деактивировать остальные")
    void linkWallet_shouldActivateExistingWallet() {
        // Arrange
        UserWalletEntity existingWallet = new UserWalletEntity();
        existingWallet.setId(1L);
        existingWallet.setUser(testUser);
        existingWallet.setWalletAddress(TEST_WALLET_ADDRESS);
        existingWallet.setIsActive(false); // Деактивирован

        when(walletRepository.findByUser_IdAndWalletAddress(TEST_USER_ID, TEST_WALLET_ADDRESS))
                .thenReturn(Optional.of(existingWallet));

        // Другие активные кошельки
        UserWalletEntity otherActiveWallet = new UserWalletEntity();
        otherActiveWallet.setId(2L);
        otherActiveWallet.setIsActive(true);
        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(otherActiveWallet));

        when(walletRepository.save(any(UserWalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserWalletEntity result = walletService.linkWallet(TEST_USER_ID, TEST_WALLET_ADDRESS, TEST_WALLET_TYPE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIsActive()).isTrue();

        // Проверяем, что другой активный кошелёк был деактивирован
        verify(walletRepository, times(1)).save(argThat(wallet -> 
            wallet.getId().equals(2L) && !wallet.getIsActive()
        ));
    }

    @Test
    @Story("Привязка кошелька")
    @DisplayName("linkWallet должен выбросить исключение если пользователь не найден")
    void linkWallet_shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(walletRepository.findByUser_IdAndWalletAddress(TEST_USER_ID, TEST_WALLET_ADDRESS))
                .thenReturn(Optional.empty());
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletService.linkWallet(TEST_USER_ID, TEST_WALLET_ADDRESS, TEST_WALLET_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найден");

        verify(walletRepository, never()).save(any());
    }

    @Test
    @Story("Получение активного кошелька")
    @DisplayName("getActiveWallet должен вернуть самый свежий активный кошелёк")
    void getActiveWallet_shouldReturnNewestActiveWallet() {
        // Arrange
        UserWalletEntity oldWallet = new UserWalletEntity();
        oldWallet.setId(1L);
        oldWallet.setCreatedAt(OffsetDateTime.now().minusDays(2));

        UserWalletEntity newWallet = new UserWalletEntity();
        newWallet.setId(2L);
        newWallet.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(oldWallet, newWallet));

        // Act
        UserWalletEntity result = walletService.getActiveWallet(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L); // Самый свежий
    }

    @Test
    @Story("Получение активного кошелька")
    @DisplayName("getActiveWallet должен выбросить WalletNotFoundException если кошелёк не найден")
    void getActiveWallet_shouldThrowExceptionWhenNotFound() {
        // Arrange
        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(new ArrayList<>());

        // Act & Assert
        assertThatThrownBy(() -> walletService.getActiveWallet(TEST_USER_ID))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("Active wallet not found");
    }

    @Test
    @Story("Получение активных кошельков")
    @DisplayName("getActiveWallets должен вернуть список активных кошельков")
    void getActiveWallets_shouldReturnListOfActiveWallets() {
        // Arrange
        UserWalletEntity wallet1 = new UserWalletEntity();
        wallet1.setId(1L);
        UserWalletEntity wallet2 = new UserWalletEntity();
        wallet2.setId(2L);

        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(wallet1, wallet2));

        // Act
        List<UserWalletEntity> result = walletService.getActiveWallets(TEST_USER_ID);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @Story("Проверка наличия активного кошелька")
    @DisplayName("hasActiveWallet должен вернуть true если у пользователя есть активный кошелёк")
    void hasActiveWallet_shouldReturnTrueWhenWalletExists() {
        // Arrange
        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(testWallet));

        // Act
        boolean result = walletService.hasActiveWallet(TEST_USER_ID);

        // Assert
        assertThat(result).isTrue();
        verify(walletRepository).findByUser_IdAndIsActiveTrue(TEST_USER_ID);
    }

    @Test
    @Story("Проверка наличия активного кошелька")
    @DisplayName("hasActiveWallet должен вернуть false если у пользователя нет активного кошелька")
    void hasActiveWallet_shouldReturnFalseWhenNoWallet() {
        // Arrange
        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(new ArrayList<>());

        // Act
        boolean result = walletService.hasActiveWallet(TEST_USER_ID);

        // Assert
        assertThat(result).isFalse();
        verify(walletRepository).findByUser_IdAndIsActiveTrue(TEST_USER_ID);
    }

    @Test
    @Story("Проверка наличия активного кошелька")
    @DisplayName("hasActiveWallet должен вернуть false если userId = null")
    void hasActiveWallet_shouldReturnFalseWhenUserIdIsNull() {
        // Act
        boolean result = walletService.hasActiveWallet(null);

        // Assert
        assertThat(result).isFalse();
        verify(walletRepository, never()).findByUser_IdAndIsActiveTrue(any());
    }

    @Test
    @Story("Отвязывание кошелька")
    @DisplayName("unlinkWallet должен деактивировать активный кошелёк")
    void unlinkWallet_shouldDeactivateActiveWallet() {
        // Arrange
        UserWalletEntity activeWallet = new UserWalletEntity();
        activeWallet.setId(1L);
        activeWallet.setUser(testUser);
        activeWallet.setWalletAddress(TEST_WALLET_ADDRESS);
        activeWallet.setIsActive(true);

        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(activeWallet));
        when(walletRepository.save(any(UserWalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        walletService.unlinkWallet(TEST_USER_ID);

        // Assert
        assertThat(activeWallet.getIsActive()).isFalse();
        verify(walletRepository).findByUser_IdAndIsActiveTrue(TEST_USER_ID);
        verify(walletRepository).save(activeWallet);
    }

    @Test
    @Story("Отвязывание кошелька")
    @DisplayName("unlinkWallet должен быть идемпотентным (можно вызывать многократно)")
    void unlinkWallet_shouldBeIdempotent() {
        // Arrange - первый вызов с активным кошельком
        UserWalletEntity activeWallet = new UserWalletEntity();
        activeWallet.setId(1L);
        activeWallet.setIsActive(true);

        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(List.of(activeWallet))
                .thenReturn(new ArrayList<>()); // второй вызов - кошелька уже нет
        when(walletRepository.save(any(UserWalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - вызываем дважды
        walletService.unlinkWallet(TEST_USER_ID);
        walletService.unlinkWallet(TEST_USER_ID);

        // Assert - не должно быть исключений, кошелёк деактивирован
        assertThat(activeWallet.getIsActive()).isFalse();
        verify(walletRepository, times(2)).findByUser_IdAndIsActiveTrue(TEST_USER_ID);
        verify(walletRepository, times(1)).save(activeWallet); // сохраняется только при первом вызове
    }

    @Test
    @Story("Отвязывание кошелька")
    @DisplayName("unlinkWallet должен корректно обрабатывать случай когда активного кошелька нет")
    void unlinkWallet_shouldLogWhenNoActiveWallet() {
        // Arrange
        when(walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID))
                .thenReturn(new ArrayList<>());

        // Act
        walletService.unlinkWallet(TEST_USER_ID);

        // Assert - не должно быть исключений, ничего не сохраняется
        verify(walletRepository).findByUser_IdAndIsActiveTrue(TEST_USER_ID);
        verify(walletRepository, never()).save(any(UserWalletEntity.class));
    }
}


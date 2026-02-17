package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.exception.StickerSetNotFoundException;
import com.example.sticker_art_gallery.exception.WalletNotFoundException;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.model.transaction.*;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.repository.transaction.TransactionIntentRepository;
import com.example.sticker_art_gallery.repository.transaction.TransactionLegRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для TransactionIntentService
 */
@ExtendWith(MockitoExtension.class)
@Epic("TON транзакции")
@Feature("Сервис намерений транзакций")
@DisplayName("Тесты TransactionIntentService")
class TransactionIntentServiceTest {

    @Mock
    private TransactionIntentRepository intentRepository;

    @Mock
    private TransactionLegRepository legRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformEntityService platformEntityService;

    @Mock
    private StickerSetRepository stickerSetRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private TransactionIntentService intentService;

    private static final Long TEST_USER_ID = 123L;
    private static final Long TEST_DONOR_USER_ID = 789L;
    private static final Long TEST_AUTHOR_ID = 456L;
    private static final Long TEST_STICKER_SET_ID = 999L;
    private static final Long TEST_SUBJECT_ENTITY_ID = 456L;
    private static final Long TEST_AMOUNT_NANO = 1_000_000_000L; // 1 TON

    private UserEntity testUser;
    private UserEntity testDonorUser;
    private UserEntity testAuthorUser;
    private PlatformEntityEntity testSubjectEntity;
    private PlatformEntityEntity testAuthorEntity;
    private StickerSet testStickerSet;
    private UserWalletEntity testAuthorWallet;

    @BeforeEach
    void setUp() {
        // Настройка тестового пользователя
        testUser = new UserEntity();
        testUser.setId(TEST_USER_ID);
        testUser.setUsername("testuser");

        // Настройка тестовой сущности
        testSubjectEntity = new PlatformEntityEntity();
        testSubjectEntity.setId(TEST_SUBJECT_ENTITY_ID);
        testSubjectEntity.setEntityType(EntityType.STICKER_SET);
        testSubjectEntity.setEntityRef("sticker_set:" + TEST_SUBJECT_ENTITY_ID);

        // Настройка донатора
        testDonorUser = new UserEntity();
        testDonorUser.setId(TEST_DONOR_USER_ID);
        testDonorUser.setUsername("donor");

        // Настройка автора
        testAuthorUser = new UserEntity();
        testAuthorUser.setId(TEST_AUTHOR_ID);
        testAuthorUser.setUsername("author");

        // Настройка PlatformEntity для автора
        testAuthorEntity = new PlatformEntityEntity();
        testAuthorEntity.setId(888L);
        testAuthorEntity.setEntityType(EntityType.USER);
        testAuthorEntity.setEntityRef("user:" + TEST_AUTHOR_ID);

        // Настройка StickerSet
        testStickerSet = new StickerSet();
        testStickerSet.setId(TEST_STICKER_SET_ID);
        testStickerSet.setUserId(TEST_AUTHOR_ID);
        testStickerSet.setIsVerified(true);
        testStickerSet.setTitle("Test StickerSet");

        // Настройка кошелька автора
        testAuthorWallet = new UserWalletEntity();
        testAuthorWallet.setId(777L);
        testAuthorWallet.setUser(testAuthorUser);
        testAuthorWallet.setWalletAddress("EQDummyAuthorWalletAddress123456789012345678901234");
        testAuthorWallet.setIsActive(true);
        testAuthorWallet.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @Story("Создание намерения транзакции")
    @DisplayName("createIntent должен создать Intent и MAIN leg в одной транзакции (Правила 1 и 2)")
    void createIntent_shouldCreateIntentAndMainLeg() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(platformEntityService.findByTypeAndRef(EntityType.STICKER_SET, "sticker_set:" + TEST_SUBJECT_ENTITY_ID))
                .thenReturn(Optional.empty());
        when(platformEntityService.getOrCreate(EntityType.STICKER_SET, "sticker_set:" + TEST_SUBJECT_ENTITY_ID, null))
                .thenReturn(testSubjectEntity);

        TransactionIntentEntity savedIntent = new TransactionIntentEntity();
        savedIntent.setId(1L);
        savedIntent.setIntentType(IntentType.DONATION);
        savedIntent.setUser(testUser);
        savedIntent.setSubjectEntity(testSubjectEntity);
        savedIntent.setAmountNano(TEST_AMOUNT_NANO);
        savedIntent.setStatus(IntentStatus.CREATED);

        when(intentRepository.save(any(TransactionIntentEntity.class))).thenReturn(savedIntent);
        when(legRepository.save(any(TransactionLegEntity.class))).thenAnswer(invocation -> {
            TransactionLegEntity leg = invocation.getArgument(0);
            leg.setId(1L);
            return leg;
        });

        // Act
        TransactionIntentEntity result = intentService.createIntent(
                IntentType.DONATION,
                TEST_USER_ID,
                TEST_SUBJECT_ENTITY_ID,
                TEST_AMOUNT_NANO,
                null
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIntentType()).isEqualTo(IntentType.DONATION);
        assertThat(result.getStatus()).isEqualTo(IntentStatus.CREATED);
        assertThat(result.getAmountNano()).isEqualTo(TEST_AMOUNT_NANO);

        // Проверяем, что Intent был сохранен
        verify(intentRepository, times(1)).save(any(TransactionIntentEntity.class));

        // Проверяем, что MAIN leg был создан (Правило 1)
        ArgumentCaptor<TransactionLegEntity> legCaptor = ArgumentCaptor.forClass(TransactionLegEntity.class);
        verify(legRepository, times(1)).save(legCaptor.capture());

        TransactionLegEntity savedLeg = legCaptor.getValue();
        assertThat(savedLeg.getLegType()).isEqualTo(LegType.MAIN);
        assertThat(savedLeg.getIntent()).isNotNull();
        assertThat(savedLeg.getToEntity()).isEqualTo(testSubjectEntity);
        assertThat(savedLeg.getAmountNano()).isEqualTo(TEST_AMOUNT_NANO);
        assertThat(savedLeg.getToWalletAddress()).isNotNull();
    }

    @Test
    @Story("Создание намерения транзакции")
    @DisplayName("createIntent должен использовать существующую PlatformEntity если она есть")
    void createIntent_shouldUseExistingPlatformEntity() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(platformEntityService.findByTypeAndRef(EntityType.STICKER_SET, "sticker_set:" + TEST_SUBJECT_ENTITY_ID))
                .thenReturn(Optional.of(testSubjectEntity));

        TransactionIntentEntity savedIntent = new TransactionIntentEntity();
        savedIntent.setId(1L);
        savedIntent.setIntentType(IntentType.DONATION);
        savedIntent.setUser(testUser);
        savedIntent.setSubjectEntity(testSubjectEntity);
        savedIntent.setAmountNano(TEST_AMOUNT_NANO);
        savedIntent.setStatus(IntentStatus.CREATED);

        when(intentRepository.save(any(TransactionIntentEntity.class))).thenReturn(savedIntent);
        when(legRepository.save(any(TransactionLegEntity.class))).thenAnswer(invocation -> {
            TransactionLegEntity leg = invocation.getArgument(0);
            leg.setId(1L);
            return leg;
        });

        // Act
        intentService.createIntent(
                IntentType.DONATION,
                TEST_USER_ID,
                TEST_SUBJECT_ENTITY_ID,
                TEST_AMOUNT_NANO,
                null
        );

        // Assert
        // Проверяем, что getOrCreate не вызывался, так как entity уже существует
        verify(platformEntityService, never()).getOrCreate(any(), any(), any());
        verify(platformEntityService, times(1)).findByTypeAndRef(any(), any());
    }

    @Test
    @Story("Создание намерения транзакции")
    @DisplayName("createIntent должен выбросить исключение если пользователь не найден")
    void createIntent_shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> intentService.createIntent(
                IntentType.DONATION,
                TEST_USER_ID,
                TEST_SUBJECT_ENTITY_ID,
                TEST_AMOUNT_NANO,
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("не найден");

        verify(intentRepository, never()).save(any());
        verify(legRepository, never()).save(any());
    }

    @Test
    @Story("Обновление статуса намерения")
    @DisplayName("updateStatus должен обновить статус намерения")
    void updateStatus_shouldUpdateIntentStatus() {
        // Arrange
        TransactionIntentEntity intent = new TransactionIntentEntity();
        intent.setId(1L);
        intent.setStatus(IntentStatus.CREATED);

        when(intentRepository.findById(1L)).thenReturn(Optional.of(intent));
        when(intentRepository.save(any(TransactionIntentEntity.class))).thenReturn(intent);

        // Act
        TransactionIntentEntity result = intentService.updateStatus(1L, IntentStatus.SENT);

        // Assert
        assertThat(result.getStatus()).isEqualTo(IntentStatus.SENT);
        verify(intentRepository, times(1)).save(intent);
    }

    @Test
    @Story("Обновление статуса намерения")
    @DisplayName("updateStatus должен выбросить исключение если намерение не найдено")
    void updateStatus_shouldThrowExceptionWhenIntentNotFound() {
        // Arrange
        when(intentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> intentService.updateStatus(1L, IntentStatus.SENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найдено");
    }

    @Test
    @Story("Получение legs для намерения")
    @DisplayName("getLegsForIntent должен вернуть список legs")
    void getLegsForIntent_shouldReturnListOfLegs() {
        // Arrange
        TransactionLegEntity leg1 = new TransactionLegEntity();
        leg1.setId(1L);
        leg1.setLegType(LegType.MAIN);

        TransactionLegEntity leg2 = new TransactionLegEntity();
        leg2.setId(2L);
        leg2.setLegType(LegType.PLATFORM_FEE);

        when(legRepository.findByIntent_Id(1L)).thenReturn(List.of(leg1, leg2));

        // Act
        List<TransactionLegEntity> result = intentService.getLegsForIntent(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLegType()).isEqualTo(LegType.MAIN);
        assertThat(result.get(1).getLegType()).isEqualTo(LegType.PLATFORM_FEE);
    }

    @Test
    @Story("Создание donation intent для стикерсета")
    @DisplayName("createStickerSetDonationIntent должен создать Intent и MAIN leg для автора")
    void createStickerSetDonationIntent_shouldCreateIntentAndMainLeg() {
        // Arrange
        when(stickerSetRepository.findById(TEST_STICKER_SET_ID)).thenReturn(Optional.of(testStickerSet));
        when(walletService.getActiveWallet(TEST_AUTHOR_ID)).thenReturn(testAuthorWallet);
        when(platformEntityService.getOrCreate(EntityType.STICKER_SET, "sticker_set:" + TEST_STICKER_SET_ID, TEST_AUTHOR_ID))
                .thenReturn(testSubjectEntity);
        when(platformEntityService.getOrCreate(EntityType.USER, "user:" + TEST_AUTHOR_ID, TEST_AUTHOR_ID))
                .thenReturn(testAuthorEntity);
        when(userRepository.findById(TEST_DONOR_USER_ID)).thenReturn(Optional.of(testDonorUser));

        TransactionIntentEntity savedIntent = new TransactionIntentEntity();
        savedIntent.setId(1L);
        savedIntent.setIntentType(IntentType.DONATION);
        savedIntent.setUser(testDonorUser);
        savedIntent.setSubjectEntity(testSubjectEntity);
        savedIntent.setAmountNano(TEST_AMOUNT_NANO);
        savedIntent.setStatus(IntentStatus.CREATED);

        when(intentRepository.save(any(TransactionIntentEntity.class))).thenReturn(savedIntent);
        when(legRepository.save(any(TransactionLegEntity.class))).thenAnswer(invocation -> {
            TransactionLegEntity leg = invocation.getArgument(0);
            leg.setId(1L);
            return leg;
        });
        when(legRepository.findByIntent_Id(1L)).thenReturn(List.of(
                createTestLeg(1L, LegType.MAIN, testAuthorWallet.getWalletAddress(), TEST_AMOUNT_NANO)
        ));

        // Act
        TransactionIntentEntity result = intentService.createStickerSetDonationIntent(
                TEST_DONOR_USER_ID,
                TEST_STICKER_SET_ID,
                TEST_AMOUNT_NANO
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIntentType()).isEqualTo(IntentType.DONATION);
        assertThat(result.getStatus()).isEqualTo(IntentStatus.CREATED);
        assertThat(result.getAmountNano()).isEqualTo(TEST_AMOUNT_NANO);
        assertThat(result.getUser().getId()).isEqualTo(TEST_DONOR_USER_ID);
        assertThat(result.getSubjectEntity()).isEqualTo(testSubjectEntity);

        // Проверяем, что Intent был сохранен
        verify(intentRepository, times(1)).save(any(TransactionIntentEntity.class));

        // Проверяем, что MAIN leg был создан с правильным адресом автора
        ArgumentCaptor<TransactionLegEntity> legCaptor = ArgumentCaptor.forClass(TransactionLegEntity.class);
        verify(legRepository, times(1)).save(legCaptor.capture());

        TransactionLegEntity savedLeg = legCaptor.getValue();
        assertThat(savedLeg.getLegType()).isEqualTo(LegType.MAIN);
        assertThat(savedLeg.getToWalletAddress()).isEqualTo(testAuthorWallet.getWalletAddress());
        assertThat(savedLeg.getToEntity()).isEqualTo(testAuthorEntity);
        assertThat(savedLeg.getAmountNano()).isEqualTo(TEST_AMOUNT_NANO);
    }

    @Test
    @Story("Создание donation intent для стикерсета")
    @DisplayName("createStickerSetDonationIntent должен выбросить StickerSetNotFoundException если стикерсет не найден")
    void createStickerSetDonationIntent_shouldThrowExceptionWhenStickerSetNotFound() {
        // Arrange
        when(stickerSetRepository.findById(TEST_STICKER_SET_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> intentService.createStickerSetDonationIntent(
                TEST_DONOR_USER_ID,
                TEST_STICKER_SET_ID,
                TEST_AMOUNT_NANO
        )).isInstanceOf(StickerSetNotFoundException.class)
          .hasMessageContaining("StickerSet not found");

        verify(intentRepository, never()).save(any());
        verify(legRepository, never()).save(any());
    }

    @Test
    @Story("Создание donation intent для стикерсета")
    @DisplayName("createStickerSetDonationIntent должен выбросить IllegalStateException если стикерсет не верифицирован")
    void createStickerSetDonationIntent_shouldThrowExceptionWhenNoAuthor() {
        // Arrange
        StickerSet stickerSetWithoutAuthor = new StickerSet();
        stickerSetWithoutAuthor.setId(TEST_STICKER_SET_ID);
        stickerSetWithoutAuthor.setIsVerified(false);

        when(stickerSetRepository.findById(TEST_STICKER_SET_ID)).thenReturn(Optional.of(stickerSetWithoutAuthor));

        // Act & Assert
        assertThatThrownBy(() -> intentService.createStickerSetDonationIntent(
                TEST_DONOR_USER_ID,
                TEST_STICKER_SET_ID,
                TEST_AMOUNT_NANO
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("не верифицирован");

        verify(walletService, never()).getActiveWallet(any());
        verify(intentRepository, never()).save(any());
    }

    @Test
    @Story("Создание donation intent для стикерсета")
    @DisplayName("createStickerSetDonationIntent должен выбросить WalletNotFoundException если у автора нет кошелька")
    void createStickerSetDonationIntent_shouldThrowExceptionWhenAuthorHasNoWallet() {
        // Arrange
        when(stickerSetRepository.findById(TEST_STICKER_SET_ID)).thenReturn(Optional.of(testStickerSet));
        when(walletService.getActiveWallet(TEST_AUTHOR_ID)).thenThrow(new WalletNotFoundException(TEST_AUTHOR_ID));

        // Act & Assert
        assertThatThrownBy(() -> intentService.createStickerSetDonationIntent(
                TEST_DONOR_USER_ID,
                TEST_STICKER_SET_ID,
                TEST_AMOUNT_NANO
        )).isInstanceOf(WalletNotFoundException.class);

        verify(intentRepository, never()).save(any());
        verify(legRepository, never()).save(any());
    }

    private TransactionLegEntity createTestLeg(Long id, LegType legType, String walletAddress, Long amountNano) {
        TransactionLegEntity leg = new TransactionLegEntity();
        leg.setId(id);
        leg.setLegType(legType);
        leg.setToWalletAddress(walletAddress);
        leg.setAmountNano(amountNano);
        return leg;
    }
}


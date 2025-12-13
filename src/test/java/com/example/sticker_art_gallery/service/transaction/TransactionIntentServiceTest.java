package com.example.sticker_art_gallery.service.transaction;

import com.example.sticker_art_gallery.model.transaction.*;
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

    @InjectMocks
    private TransactionIntentService intentService;

    private static final Long TEST_USER_ID = 123L;
    private static final Long TEST_SUBJECT_ENTITY_ID = 456L;
    private static final Long TEST_AMOUNT_NANO = 1_000_000_000L; // 1 TON

    private UserEntity testUser;
    private PlatformEntityEntity testSubjectEntity;

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
}


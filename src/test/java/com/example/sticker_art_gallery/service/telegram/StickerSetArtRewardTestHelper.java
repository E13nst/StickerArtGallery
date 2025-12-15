package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import io.qameta.allure.Step;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper класс для тестов начисления ART за стикерсеты.
 * Упрощает написание тестов и улучшает их читаемость.
 */
@Component
public class StickerSetArtRewardTestHelper {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ArtTransactionRepository artTransactionRepository;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Настраивает пользователя с нулевым балансом ART.
     * Создает профиль, если его нет, или сбрасывает баланс до 0.
     */
    @Step("Настроить пользователя {userId} с нулевым балансом ART")
    public void setupUserWithZeroBalance(Long userId) {
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfileEntity entity = new UserProfileEntity();
                    entity.setUserId(userId);
                    entity.setRole(UserProfileEntity.UserRole.USER);
                    entity.setArtBalance(0L);
                    entity.setIsBlocked(false);
                    return userProfileRepository.save(entity);
                });
        profile.setArtBalance(0L);
        userProfileRepository.save(profile);
    }

    /**
     * Получает начальный баланс пользователя.
     * Используется для проверки изменений баланса в тестах.
     */
    @Step("Получить начальный баланс ART пользователя {userId}")
    public long getInitialBalance(Long userId) {
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Пользователь " + userId + " не найден"));
        return profile.getArtBalance();
    }

    /**
     * Мокирует валидацию стикерсета в Telegram API.
     * Настраивает моки для validateStickerSetExists и extractTitleFromStickerSetInfo.
     * 
     * @param telegramBotApiService мок сервиса Telegram API
     * @param normalizedName нормализованное имя стикерсета (без URL)
     * @param title заголовок стикерсета, который вернет мок
     */
    @Step("Настроить мок Telegram API для стикерсета '{normalizedName}' с заголовком '{title}'")
    public void mockTelegramStickerSetValidation(TelegramBotApiService telegramBotApiService, String normalizedName, String title) {
        Object telegramInfo = new Object();
        org.mockito.Mockito.when(telegramBotApiService.validateStickerSetExists(normalizedName)).thenReturn(telegramInfo);
        org.mockito.Mockito.when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn(title);
    }

    /**
     * Создает DTO для публичного стикерсета.
     * Публичные стикерсеты начисляют ART при создании.
     */
    @Step("Создать DTO для публичного стикерсета '{name}'")
    public CreateStickerSetDto createPublicStickerSetDto(String name) {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(name);
        dto.setVisibility(StickerSetVisibility.PUBLIC);
        return dto;
    }

    /**
     * Создает DTO для приватного стикерсета.
     * Приватные стикерсеты НЕ начисляют ART при создании, только при публикации.
     */
    @Step("Создать DTO для приватного стикерсета '{name}'")
    public CreateStickerSetDto createPrivateStickerSetDto(String name) {
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(name);
        dto.setVisibility(StickerSetVisibility.PRIVATE);
        return dto;
    }

    /**
     * Выполняет flush и clear EntityManager.
     * Необходимо для сброса кеша Hibernate и получения актуальных данных из БД.
     * Используется после операций, которые изменяют состояние сущностей.
     */
    @Step("Сбросить кеш Hibernate (flush и clear)")
    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Проверяет транзакцию ART за загрузку стикерсета.
     * 
     * @param userId ID пользователя
     * @param stickerSetId ID стикерсета
     * @param expectedDelta ожидаемое изменение баланса (обычно 10)
     */
    @Step("Проверить транзакцию ART за загрузку стикерсета {stickerSetId} пользователем {userId}")
    public void verifyArtTransactionForUpload(Long userId, Long stickerSetId, long expectedDelta) {
        String expectedExternalId = String.format("sticker-upload:%d:%d", userId, stickerSetId);
        Optional<ArtTransactionEntity> transactionOpt = artTransactionRepository.findByExternalId(expectedExternalId);
        
        assertThat(transactionOpt)
                .as("Транзакция за загрузку стикерсета должна существовать")
                .isPresent();

        ArtTransactionEntity transaction = transactionOpt.get();
        assertThat(transaction.getRuleCode())
                .as("Код правила должен быть UPLOAD_STICKERSET")
                .isEqualTo(ArtRewardService.RULE_UPLOAD_STICKERSET);
        assertThat(transaction.getDelta())
                .as("Изменение баланса должно быть " + expectedDelta)
                .isEqualTo(expectedDelta);
        
        // Проверяем метаданные
        assertThat(transaction.getMetadata())
                .as("Метаданные должны содержать stickerSetId")
                .contains("stickerSetId")
                .contains(String.valueOf(stickerSetId));
    }

    /**
     * Проверяет транзакцию ART за публикацию стикерсета.
     * 
     * @param stickerSetName имя стикерсета (нормализованное)
     * @param stickerSetId ID стикерсета
     * @param expectedDelta ожидаемое изменение баланса (обычно 10)
     */
    @Step("Проверить транзакцию ART за публикацию стикерсета '{stickerSetName}'")
    public void verifyArtTransactionForPublish(String stickerSetName, Long stickerSetId, long expectedDelta) {
        String expectedExternalId = "sticker-publish:" + stickerSetName;
        Optional<ArtTransactionEntity> transactionOpt = artTransactionRepository.findByExternalId(expectedExternalId);
        
        assertThat(transactionOpt)
                .as("Транзакция за публикацию стикерсета должна существовать")
                .isPresent();

        ArtTransactionEntity transaction = transactionOpt.get();
        assertThat(transaction.getRuleCode())
                .as("Код правила должен быть PUBLISH_STICKERSET")
                .isEqualTo(ArtRewardService.RULE_PUBLISH_STICKERSET);
        assertThat(transaction.getDelta())
                .as("Изменение баланса должно быть " + expectedDelta)
                .isEqualTo(expectedDelta);
        
        // Проверяем метаданные
        assertThat(transaction.getMetadata())
                .as("Метаданные должны содержать stickerSetId и name")
                .contains("stickerSetId")
                .contains("name")
                .contains(String.valueOf(stickerSetId))
                .contains(stickerSetName);
    }

    /**
     * Проверяет, что баланс пользователя изменился на ожидаемую величину.
     * 
     * @param userId ID пользователя
     * @param initialBalance начальный баланс
     * @param expectedDelta ожидаемое изменение баланса
     * @param contextMessage сообщение для контекста (что проверяем)
     */
    @Step("Проверить изменение баланса ART пользователя {userId} на {expectedDelta}")
    public void verifyBalanceChange(Long userId, long initialBalance, long expectedDelta, String contextMessage) {
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Пользователь " + userId + " не найден"));
        
        assertThat(profile.getArtBalance())
                .as(contextMessage != null ? contextMessage : "Баланс должен измениться на " + expectedDelta)
                .isEqualTo(initialBalance + expectedDelta);
    }

    /**
     * Проверяет, что баланс пользователя не изменился.
     * 
     * @param userId ID пользователя
     * @param initialBalance начальный баланс
     * @param contextMessage сообщение для контекста (что проверяем)
     */
    @Step("Проверить, что баланс ART пользователя {userId} не изменился")
    public void verifyBalanceUnchanged(Long userId, long initialBalance, String contextMessage) {
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Пользователь " + userId + " не найден"));
        
        assertThat(profile.getArtBalance())
                .as(contextMessage != null ? contextMessage : "Баланс не должен измениться")
                .isEqualTo(initialBalance);
    }

    /**
     * Проверяет, что транзакций ART для пользователя нет или их определенное количество.
     * 
     * @param userId ID пользователя
     * @param expectedCount ожидаемое количество транзакций
     * @param contextMessage сообщение для контекста
     */
    @Step("Проверить количество транзакций ART пользователя {userId}")
    public void verifyTransactionCount(Long userId, long expectedCount, String contextMessage) {
        long actualCount = artTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, 
                org.springframework.data.domain.PageRequest.of(0, 100))
                .getTotalElements();
        
        assertThat(actualCount)
                .as(contextMessage != null ? contextMessage : "Количество транзакций должно быть " + expectedCount)
                .isEqualTo(expectedCount);
    }

    /**
     * Проверяет, что стикерсет имеет ожидаемую видимость.
     */
    @Step("Проверить видимость стикерсета {stickerSetId}")
    public void verifyStickerSetVisibility(StickerSet stickerSet, StickerSetVisibility expectedVisibility) {
        assertThat(stickerSet.getVisibility())
                .as("Стикерсет должен иметь видимость " + expectedVisibility)
                .isEqualTo(expectedVisibility);
    }

    /**
     * Очищает тестовые данные: удаляет стикерсет и все транзакции.
     */
    @Step("Очистить тестовые данные для стикерсета '{normalizedName}'")
    public void cleanupTestData(String normalizedName) {
        stickerSetRepository.findByNameIgnoreCase(normalizedName)
                .ifPresent(stickerSetRepository::delete);
        artTransactionRepository.deleteAll();
    }
}


package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("Стикерсеты")
@Feature("Состояния и публикация")
@DisplayName("Интеграция: состояния стикерсетов и публикация")
@Tag("integration")
class StickerSetStateAndPublishIntegrationTest {

    private static final Long USER_ID = TestDataBuilder.TEST_USER_ID;
    private static final String STICKERSET_NAME = "https://t.me/addstickers/test_state_pack";
    private static final String NORMALIZED_NAME = "test_state_pack";

    @Autowired
    private StickerSetService stickerSetService;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    @Autowired
    private ArtTransactionRepository artTransactionRepository;

    @Autowired
    private ArtRuleService artRuleService;

    @Autowired
    private StickerSetArtRewardTestHelper testHelper;

    @MockBean
    private TelegramBotApiService telegramBotApiService;

    @BeforeEach
    void setUp() {
        // Очищаем тестовые данные для стабильности теста
        testHelper.cleanupTestData(NORMALIZED_NAME);
        
        // Настраиваем пользователя с нулевым балансом ART
        testHelper.setupUserWithZeroBalance(USER_ID);

        // Убеждаемся, что нужные правила существуют
        Assertions.assertThat(artRuleService.getAllRules())
                .anyMatch(rule -> rule.getCode().equals(ArtRewardService.RULE_UPLOAD_STICKERSET));
        Assertions.assertThat(artRuleService.getAllRules())
                .anyMatch(rule -> rule.getCode().equals(ArtRewardService.RULE_PUBLISH_STICKERSET));
    }

    @Test
    @Story("Восстановление удаленного стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Восстановление DELETED стикерсета не должно начислять ART повторно")
    void restoreDeletedStickerSet_ShouldNotAwardArtPoints() {
        // given: получаем начальный баланс и создаем тестовый стикерсет
        // Бизнес-правило: при восстановлении удаленного стикерсета ART НЕ начисляются повторно
        // ART начисляются только при первом создании (не восстановлении)
        long initialBalance = testHelper.getInitialBalance(USER_ID);
        
        // Шаг 1: Создаем публичный стикерсет (начисляется 10 ART)
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "Test Pack");
        CreateStickerSetDto createDto = testHelper.createPublicStickerSetDto(STICKERSET_NAME);
        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();
        testHelper.flushAndClear();

        // Проверяем промежуточное состояние: ART должны быть начислены за первое создание
        testHelper.verifyBalanceChange(
            USER_ID, 
            initialBalance, 
            10L, 
            "После создания публичного стикерсета баланс должен увеличиться на 10 ART"
        );

        // Шаг 2: Удаляем стикерсет (soft delete)
        // Стикерсет переходит в состояние DELETED, но запись остается в БД
        stickerSetService.deleteById(stickerSetId);
        testHelper.flushAndClear();

        // Проверяем, что стикерсет действительно удален
        StickerSet deleted = stickerSetRepository.findById(stickerSetId).orElseThrow();
        assertThat(deleted.getState())
                .as("Стикерсет должен быть в состоянии DELETED")
                .isEqualTo(StickerSetState.DELETED);
        assertThat(deleted.getDeletedAt())
                .as("У удаленного стикерсета должен быть установлен deletedAt")
                .isNotNull();

        // when: восстанавливаем стикерсет
        // При восстановлении система определяет, что это существующий DELETED стикерсет
        // и восстанавливает его БЕЗ начисления ART (флаг isRestored = true)
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "Test Pack Restored");
        CreateStickerSetDto restoreDto = testHelper.createPublicStickerSetDto(STICKERSET_NAME);
        StickerSet restored = stickerSetService.createStickerSetForUser(restoreDto, USER_ID, "ru", null);
        testHelper.flushAndClear();

        // then: проверяем результаты восстановления
        // 1. Баланс не должен измениться дополнительно (только 10 ART за первое создание)
        testHelper.verifyBalanceChange(
            USER_ID, 
            initialBalance, 
            10L, 
            "Баланс не должен измениться при восстановлении - только 10 ART за первое создание"
        );

        // 2. Стикерсет должен быть восстановлен (ACTIVE, без deletedAt, тот же ID)
        assertThat(restored.getState())
                .as("Восстановленный стикерсет должен быть в состоянии ACTIVE")
                .isEqualTo(StickerSetState.ACTIVE);
        assertThat(restored.getDeletedAt())
                .as("У восстановленного стикерсета не должно быть deletedAt")
                .isNull();
        assertThat(restored.getId())
                .as("Восстановленный стикерсет должен иметь тот же ID")
                .isEqualTo(stickerSetId);

        // 3. Должна быть только одна транзакция (за первое создание, не за восстановление)
        testHelper.verifyTransactionCount(
            USER_ID, 
            1L, 
            "Должна быть только одна транзакция ART - за первое создание, не за восстановление"
        );
    }

    @Test
    @Story("Публикация стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Публикация PRIVATE стикерсета должна начислять ART за первую публикацию")
    void publishPrivateStickerSet_ShouldAwardArtPoints() {
        // given: получаем начальный баланс и создаем приватный стикерсет
        // Бизнес-правило: 
        // 1. PRIVATE стикерсеты НЕ начисляют ART при создании
        // 2. При первой публикации (PRIVATE -> PUBLIC) начисляется 10 ART
        long initialBalance = testHelper.getInitialBalance(USER_ID);

        // Шаг 1: Создаем приватный стикерсет
        // При создании PRIVATE стикерсета ART не начисляются
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "Test Pack");
        CreateStickerSetDto createDto = testHelper.createPrivateStickerSetDto(STICKERSET_NAME);
        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();
        testHelper.flushAndClear();

        // Проверяем промежуточное состояние: баланс не должен измениться
        testHelper.verifyBalanceUnchanged(
            USER_ID, 
            initialBalance, 
            "Баланс не должен измениться при создании приватного стикерсета"
        );

        // when: публикуем стикерсет (PRIVATE -> PUBLIC)
        // При публикации система проверяет, была ли уже публикация этого стикерсета
        // Если это первая публикация, начисляется 10 ART
        StickerSet published = stickerSetService.publishStickerSet(stickerSetId);
        testHelper.flushAndClear();

        // then: проверяем результаты публикации
        // 1. Баланс должен увеличиться на 10 ART за публикацию
        testHelper.verifyBalanceChange(
            USER_ID, 
            initialBalance, 
            10L, 
            "Баланс должен увеличиться на 10 ART за первую публикацию стикерсета"
        );

        // 2. Должна быть создана транзакция ART за публикацию
        testHelper.verifyArtTransactionForPublish(NORMALIZED_NAME, stickerSetId, 10L);

        // 3. Стикерсет должен стать публичным
        testHelper.verifyStickerSetVisibility(published, StickerSetVisibility.PUBLIC);
    }

    @Test
    @Story("Публикация стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Повторная публикация не должна начислять ART повторно")
    void republishStickerSet_ShouldNotAwardArtPointsAgain() {
        // given: получаем начальный баланс и создаем приватный стикерсет
        // Бизнес-правило: ART за публикацию начисляются только один раз за стикерсет
        // Повторные публикации (после unpublish) не начисляют ART
        long initialBalance = testHelper.getInitialBalance(USER_ID);

        // Шаг 1: Создаем приватный стикерсет
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "Test Pack");
        CreateStickerSetDto createDto = testHelper.createPrivateStickerSetDto(STICKERSET_NAME);
        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();

        // Шаг 2: Первая публикация (PRIVATE -> PUBLIC)
        // При первой публикации начисляется 10 ART
        stickerSetService.publishStickerSet(stickerSetId);
        testHelper.flushAndClear();

        // Проверяем промежуточное состояние: баланс должен увеличиться на 10 ART
        long balanceAfterFirstPublish = testHelper.getInitialBalance(USER_ID);
        assertThat(balanceAfterFirstPublish)
                .as("После первой публикации баланс должен быть " + (initialBalance + 10L))
                .isEqualTo(initialBalance + 10L);

        // Шаг 3: Делаем стикерсет снова приватным (PUBLIC -> PRIVATE)
        // Это не влияет на баланс, просто меняет видимость
        stickerSetService.unpublishStickerSet(stickerSetId);
        testHelper.flushAndClear();

        // when: публикуем стикерсет повторно (PRIVATE -> PUBLIC)
        // Система проверяет, была ли уже публикация этого стикерсета
        // Поскольку публикация уже была, ART НЕ начисляются повторно
        stickerSetService.publishStickerSet(stickerSetId);
        testHelper.flushAndClear();

        // then: проверяем, что ART не начислены повторно
        // 1. Баланс не должен измениться дополнительно
        testHelper.verifyBalanceChange(
            USER_ID, 
            initialBalance, 
            10L, 
            "Баланс не должен измениться при повторной публикации - только 10 ART за первую публикацию"
        );

        // 2. Должна быть только одна транзакция за публикацию
        long publishTransactionCount = artTransactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .filter(tx -> tx.getRuleCode().equals(ArtRewardService.RULE_PUBLISH_STICKERSET))
                .count();
        assertThat(publishTransactionCount)
                .as("Должна быть только одна транзакция ART за публикацию (за первую публикацию)")
                .isEqualTo(1);
    }

    @Test
    @Story("Блокировка стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Попытка загрузить BLOCKED стикерсет должна выбросить исключение")
    void createStickerSet_WithBlockedStickerSet_ShouldThrowException() {
        // given: создаем и блокируем стикерсет
        // Бизнес-правило: заблокированные стикерсеты нельзя загружать повторно
        // Система должна выбросить исключение с причиной блокировки
        
        // Шаг 1: Создаем публичный стикерсет
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "Test Pack");
        CreateStickerSetDto createDto = testHelper.createPublicStickerSetDto(STICKERSET_NAME);
        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();

        // Шаг 2: Блокируем стикерсет администратором
        // Стикерсет переходит в состояние BLOCKED с указанием причины
        String blockReason = "Нарушение правил";
        stickerSetService.blockStickerSet(stickerSetId, blockReason);
        testHelper.flushAndClear();

        // Проверяем промежуточное состояние: стикерсет должен быть заблокирован
        StickerSet blocked = stickerSetRepository.findById(stickerSetId).orElseThrow();
        assertThat(blocked.getState())
                .as("Стикерсет должен быть в состоянии BLOCKED")
                .isEqualTo(StickerSetState.BLOCKED);
        assertThat(blocked.getBlockReason())
                .as("У заблокированного стикерсета должна быть указана причина")
                .isEqualTo(blockReason);

        // when & then: попытка загрузить заблокированный стикерсет должна выбросить исключение
        // Система проверяет существующий стикерсет и видит, что он заблокирован
        // Выбрасывается IllegalArgumentException с сообщением о блокировке и причиной
        CreateStickerSetDto newDto = testHelper.createPublicStickerSetDto(STICKERSET_NAME);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            stickerSetService.createStickerSetForUser(newDto, USER_ID, "ru", null);
        })
                .as("Должно быть выброшено исключение при попытке загрузить заблокированный стикерсет")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("был заблокирован")
                .hasMessageContaining(blockReason);
    }
}


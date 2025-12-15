package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("ART-баллы")
@Feature("Начисление при создании стикерсета")
@DisplayName("Интеграция StickerSetService ↔️ ART награды")
@Tag("integration")
class StickerSetArtRewardIntegrationTest {

    private static final Long USER_ID = TestDataBuilder.TEST_USER_ID;
    private static final String STICKERSET_NAME = "https://t.me/addstickers/taxiderm";
    private static final String NORMALIZED_NAME = "taxiderm";

    @Autowired
    private StickerSetService stickerSetService;

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

        // Убеждаемся, что нужное правило существует
        Assertions.assertThat(artRuleService.getAllRules())
                .anyMatch(rule -> rule.getCode().equals(ArtRewardService.RULE_UPLOAD_STICKERSET));
    }

    @Test
    @Story("Начисление ART при успешном добавлении стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Создание публичного стикерсета должно начислить 10 ART пользователю")
    void createStickerSetForUser_shouldAwardArtPoints() {
        // given: получаем начальный баланс и настраиваем тестовые данные
        // Бизнес-правило: при создании PUBLIC стикерсета пользователю начисляется 10 ART
        long initialBalance = testHelper.getInitialBalance(USER_ID);
        
        // Настраиваем мок Telegram API для валидации стикерсета
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "taxiderm");
        
        // Создаем DTO для публичного стикерсета
        // Важно: только PUBLIC стикерсеты начисляют ART при создании
        CreateStickerSetDto dto = testHelper.createPublicStickerSetDto(STICKERSET_NAME);

        // when: создаем стикерсет
        // Внутри сервиса происходит:
        // 1. Валидация стикерсета через Telegram API
        // 2. Сохранение стикерсета в БД
        // 3. Начисление 10 ART пользователю (только для PUBLIC)
        StickerSet created = stickerSetService.createStickerSetForUser(dto, USER_ID, "ru", null);

        // Сбрасываем кеш Hibernate, чтобы получить актуальные данные из БД
        // Это необходимо, так как начисление ART происходит в отдельной транзакции
        testHelper.flushAndClear();

        // then: проверяем результаты
        // 1. Баланс должен увеличиться на 10 ART
        testHelper.verifyBalanceChange(
            USER_ID, 
            initialBalance, 
            10L, 
            "Баланс пользователя должен увеличиться на 10 ART после создания публичного стикерсета"
        );

        // 2. Должна быть создана транзакция ART с правильными параметрами
        testHelper.verifyArtTransactionForUpload(USER_ID, created.getId(), 10L);

        // 3. Проверяем, что создана только одна транзакция
        testHelper.verifyTransactionCount(
            USER_ID, 
            1L, 
            "Должна быть создана ровно одна транзакция ART за загрузку стикерсета"
        );
    }

    @Test
    @Story("Начисление ART при создании приватного стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Создание PRIVATE стикерсета НЕ должно начислять ART")
    void createStickerSetForUser_WithPrivateStickerSet_ShouldNotAwardArtPoints() {
        // given: получаем начальный баланс и настраиваем тестовые данные
        // Бизнес-правило: PRIVATE стикерсеты НЕ начисляют ART при создании
        // ART начисляется только при публикации (PRIVATE -> PUBLIC)
        long initialBalance = testHelper.getInitialBalance(USER_ID);
        
        // Настраиваем мок Telegram API для валидации стикерсета
        testHelper.mockTelegramStickerSetValidation(telegramBotApiService, NORMALIZED_NAME, "taxiderm");
        
        // Создаем DTO для приватного стикерсета
        // Важно: PRIVATE стикерсеты не начисляют ART при создании
        CreateStickerSetDto dto = testHelper.createPrivateStickerSetDto(STICKERSET_NAME);

        // when: создаем приватный стикерсет
        // Внутри сервиса происходит:
        // 1. Валидация стикерсета через Telegram API
        // 2. Сохранение стикерсета в БД
        // 3. ART НЕ начисляется, так как visibility = PRIVATE
        stickerSetService.createStickerSetForUser(dto, USER_ID, "ru", null);

        // Сбрасываем кеш Hibernate для получения актуальных данных
        testHelper.flushAndClear();

        // then: проверяем, что ART не начислены
        // 1. Баланс не должен измениться
        testHelper.verifyBalanceUnchanged(
            USER_ID, 
            initialBalance, 
            "Баланс пользователя не должен измениться при создании приватного стикерсета"
        );

        // 2. Не должно быть создано транзакций ART
        testHelper.verifyTransactionCount(
            USER_ID, 
            0L, 
            "Не должно быть транзакций ART для приватного стикерсета"
        );
    }
}


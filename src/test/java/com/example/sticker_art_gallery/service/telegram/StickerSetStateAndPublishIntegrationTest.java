package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.*;
import jakarta.persistence.EntityManager;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ArtRuleService artRuleService;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private TelegramBotApiService telegramBotApiService;

    @BeforeEach
    void setUp() {
        // Чистим предыдущие данные для стабильности теста
        stickerSetRepository.findByNameIgnoreCase(NORMALIZED_NAME)
                .ifPresent(stickerSetRepository::delete);
        artTransactionRepository.deleteAll();

        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID)
                .orElseGet(() -> {
                    UserProfileEntity entity = new UserProfileEntity();
                    entity.setUserId(USER_ID);
                    entity.setRole(UserProfileEntity.UserRole.USER);
                    entity.setArtBalance(0L);
                    entity.setIsBlocked(false);
                    return userProfileRepository.save(entity);
                });
        profile.setArtBalance(0L);
        userProfileRepository.save(profile);

        // Убеждаемся, что нужные правила существуют
        Assertions.assertThat(artRuleService.getAllRules())
                .anyMatch(rule -> rule.getCode().equals(ArtRewardService.RULE_UPLOAD_STICKERSET));
        Assertions.assertThat(artRuleService.getAllRules())
                .anyMatch(rule -> rule.getCode().equals(ArtRewardService.RULE_PUBLISH_STICKERSET));
    }

    @Test
    @Story("Восстановление удаленного стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Восстановление DELETED стикерсета не должно начислять ART")
    void restoreDeletedStickerSet_ShouldNotAwardArtPoints() {
        // Получаем начальный баланс
        UserProfileEntity initialProfile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long initialBalance = initialProfile.getArtBalance();
        
        // Убеждаемся, что начальный баланс известен
        assertThat(initialBalance).isNotNull();

        // given: создаем и удаляем стикерсет
        Object telegramInfo = new Object();
        when(telegramBotApiService.validateStickerSetExists(NORMALIZED_NAME)).thenReturn(telegramInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn("Test Pack");

        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName(STICKERSET_NAME);
        createDto.setVisibility(StickerSetVisibility.PUBLIC);

        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();

        // Удаляем стикерсет (soft delete)
        stickerSetService.deleteById(stickerSetId);
        entityManager.flush();
        entityManager.clear();

        // Проверяем, что стикерсет удален
        StickerSet deleted = stickerSetRepository.findById(stickerSetId).orElseThrow();
        assertThat(deleted.getState()).isEqualTo(StickerSetState.DELETED);
        assertThat(deleted.getDeletedAt()).isNotNull();

        // when: восстанавливаем стикерсет
        CreateStickerSetDto restoreDto = new CreateStickerSetDto();
        restoreDto.setName(STICKERSET_NAME);
        restoreDto.setVisibility(StickerSetVisibility.PUBLIC);

        when(telegramBotApiService.validateStickerSetExists(NORMALIZED_NAME)).thenReturn(telegramInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn("Test Pack Restored");

        StickerSet restored = stickerSetService.createStickerSetForUser(restoreDto, USER_ID, "ru", null);

        entityManager.flush();
        entityManager.clear();

        // then: баланс не должен измениться (ART не начисляется при восстановлении)
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(profile.getArtBalance())
                .as("Баланс не должен измениться при восстановлении")
                .isEqualTo(initialBalance + 10L); // Только за первое создание

        // Проверяем, что стикерсет восстановлен
        assertThat(restored.getState()).isEqualTo(StickerSetState.ACTIVE);
        assertThat(restored.getDeletedAt()).isNull();
        assertThat(restored.getId()).isEqualTo(stickerSetId); // Тот же ID

        // Проверяем, что транзакций только одна (за первое создание)
        var txPage = artTransactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 10));
        assertThat(txPage.getTotalElements())
                .as("Должна быть только одна транзакция (за первое создание)")
                .isEqualTo(1);
    }

    @Test
    @Story("Публикация стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Публикация PRIVATE стикерсета должна начислять ART за первую публикацию")
    void publishPrivateStickerSet_ShouldAwardArtPoints() {
        // Получаем начальный баланс
        UserProfileEntity initialProfile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long initialBalance = initialProfile.getArtBalance();
        
        // Убеждаемся, что начальный баланс известен
        assertThat(initialBalance).isNotNull();

        // given: создаем приватный стикерсет
        Object telegramInfo = new Object();
        when(telegramBotApiService.validateStickerSetExists(NORMALIZED_NAME)).thenReturn(telegramInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn("Test Pack");

        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName(STICKERSET_NAME);
        createDto.setVisibility(StickerSetVisibility.PRIVATE); // Приватный

        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();

        entityManager.flush();
        entityManager.clear();

        // Проверяем, что ART не начислены при создании приватного
        UserProfileEntity beforePublish = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(beforePublish.getArtBalance())
                .as("Баланс не должен измениться при создании приватного стикерсета")
                .isEqualTo(initialBalance);

        // when: публикуем стикерсет
        StickerSet published = stickerSetService.publishStickerSet(stickerSetId);

        entityManager.flush();
        entityManager.clear();

        // then: должен быть начислен ART за публикацию
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(profile.getArtBalance())
                .as("Баланс должен увеличиться на 10 ART за публикацию")
                .isEqualTo(initialBalance + 10L);

        // Проверяем транзакцию
        String expectedExternalId = "sticker-publish:" + NORMALIZED_NAME;
        Optional<ArtTransactionEntity> transactionOpt = artTransactionRepository.findByExternalId(expectedExternalId);
        assertThat(transactionOpt)
                .as("Транзакция с ожидаемым externalId")
                .isPresent();

        ArtTransactionEntity transaction = transactionOpt.get();
        assertThat(transaction.getRuleCode()).isEqualTo(ArtRewardService.RULE_PUBLISH_STICKERSET);
        assertThat(transaction.getDelta()).isEqualTo(10L);
        assertThat(transaction.getBalanceAfter()).isEqualTo(profile.getArtBalance());
        assertThat(transaction.getMetadata())
                .as("Метаданные должны содержать stickerSetId и name")
                .contains("stickerSetId")
                .contains("name")
                .contains(String.valueOf(stickerSetId))
                .contains(NORMALIZED_NAME);

        // Проверяем, что стикерсет стал публичным
        assertThat(published.getVisibility()).isEqualTo(StickerSetVisibility.PUBLIC);
    }

    @Test
    @Story("Публикация стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Повторная публикация не должна начислять ART повторно")
    void republishStickerSet_ShouldNotAwardArtPointsAgain() {
        // Получаем начальный баланс
        UserProfileEntity initialProfile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long initialBalance = initialProfile.getArtBalance();
        
        // Убеждаемся, что начальный баланс известен
        assertThat(initialBalance).isNotNull();

        // given: создаем приватный стикерсет и публикуем его
        Object telegramInfo = new Object();
        when(telegramBotApiService.validateStickerSetExists(NORMALIZED_NAME)).thenReturn(telegramInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn("Test Pack");

        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName(STICKERSET_NAME);
        createDto.setVisibility(StickerSetVisibility.PRIVATE);

        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();

        // Первая публикация
        stickerSetService.publishStickerSet(stickerSetId);
        entityManager.flush();
        entityManager.clear();

        // Проверяем баланс после первой публикации
        UserProfileEntity afterFirstPublish = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        long balanceAfterFirstPublish = afterFirstPublish.getArtBalance();

        // Делаем приватным
        stickerSetService.unpublishStickerSet(stickerSetId);
        entityManager.flush();
        entityManager.clear();

        // when: публикуем повторно
        stickerSetService.publishStickerSet(stickerSetId);
        entityManager.flush();
        entityManager.clear();

        // then: баланс не должен измениться (ART не начисляется повторно)
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(profile.getArtBalance())
                .as("Баланс не должен измениться при повторной публикации")
                .isEqualTo(balanceAfterFirstPublish);

        // Проверяем, что транзакций за публикацию только одна
        var publishTransactions = artTransactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .filter(tx -> tx.getRuleCode().equals(ArtRewardService.RULE_PUBLISH_STICKERSET))
                .count();
        assertThat(publishTransactions)
                .as("Должна быть только одна транзакция за публикацию")
                .isEqualTo(1);
    }

    @Test
    @Story("Блокировка стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Попытка загрузить BLOCKED стикерсет должна выбросить исключение")
    void createStickerSet_WithBlockedStickerSet_ShouldThrowException() {
        // given: создаем и блокируем стикерсет
        Object telegramInfo = new Object();
        when(telegramBotApiService.validateStickerSetExists(NORMALIZED_NAME)).thenReturn(telegramInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn("Test Pack");

        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName(STICKERSET_NAME);
        createDto.setVisibility(StickerSetVisibility.PUBLIC);

        StickerSet created = stickerSetService.createStickerSetForUser(createDto, USER_ID, "ru", null);
        Long stickerSetId = created.getId();

        // Блокируем стикерсет
        stickerSetService.blockStickerSet(stickerSetId, "Нарушение правил");
        entityManager.flush();
        entityManager.clear();

        // Проверяем, что стикерсет заблокирован
        StickerSet blocked = stickerSetRepository.findById(stickerSetId).orElseThrow();
        assertThat(blocked.getState()).isEqualTo(StickerSetState.BLOCKED);

        // when & then: попытка загрузить заблокированный стикерсет должна выбросить исключение
        CreateStickerSetDto newDto = new CreateStickerSetDto();
        newDto.setName(STICKERSET_NAME);
        newDto.setVisibility(StickerSetVisibility.PUBLIC);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            stickerSetService.createStickerSetForUser(newDto, USER_ID, "ru", null);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("был заблокирован")
                .hasMessageContaining("Нарушение правил");
    }
}


package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

        // Убеждаемся, что нужное правило существует
        Assertions.assertThat(artRuleService.getAllRules())
                .anyMatch(rule -> rule.getCode().equals(ArtRewardService.RULE_UPLOAD_STICKERSET));
    }

    @Test
    @Story("Начисление ART при успешном добавлении стикерсета")
    @Severity(SeverityLevel.CRITICAL)
    void createStickerSetForUser_shouldAwardArtPoints() {
        // given
        Object telegramInfo = new Object();
        when(telegramBotApiService.validateStickerSetExists(NORMALIZED_NAME)).thenReturn(telegramInfo);
        when(telegramBotApiService.extractTitleFromStickerSetInfo(telegramInfo)).thenReturn("taxiderm");

        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(STICKERSET_NAME);

        // when
        StickerSet created = stickerSetService.createStickerSetForUser(dto, USER_ID, "ru", null);

        entityManager.flush();
        entityManager.clear();

        // then
        UserProfileEntity profile = userProfileRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(profile.getArtBalance())
                .as("Баланс пользователя после создания стикерсета")
                .isEqualTo(10L);

        String expectedExternalId = String.format("sticker-upload:%d:%d", USER_ID, created.getId());
        Optional<ArtTransactionEntity> transactionOpt = artTransactionRepository.findByExternalId(expectedExternalId);
        assertThat(transactionOpt)
                .as("Транзакция с ожидаемым externalId")
                .isPresent();

        ArtTransactionEntity transaction = transactionOpt.get();
        assertThat(transaction.getRuleCode()).isEqualTo(ArtRewardService.RULE_UPLOAD_STICKERSET);
        assertThat(transaction.getDelta()).isEqualTo(10L);
        assertThat(transaction.getBalanceAfter()).isEqualTo(10L);
        assertThat(transaction.getMetadata()).contains("\"stickerSetId\":" + created.getId());

        // убеждаемся, что кеш правил использовался без ошибок
        var txPage = artTransactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 5));
        assertThat(txPage.getTotalElements()).isEqualTo(1);
    }
}


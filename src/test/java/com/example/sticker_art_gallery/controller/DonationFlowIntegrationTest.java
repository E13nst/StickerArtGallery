package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.transaction.PrepareTransactionRequest;
import com.example.sticker_art_gallery.dto.transaction.PrepareTransactionResponse;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.service.transaction.WalletService;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для donation flow
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("TON Donations")
@Feature("Интеграция: Donation Flow")
@DisplayName("Интеграционные тесты Donation Flow")
@org.junit.jupiter.api.Tag("integration")
@org.junit.jupiter.api.Tag("ton")
class DonationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private com.example.sticker_art_gallery.config.AppConfig appConfig;
    
    @Autowired
    private com.example.sticker_art_gallery.model.profile.UserProfileRepository userProfileRepository;

    private StickerSetTestSteps testSteps;
    private UserEntity authorUser;
    private StickerSet testStickerSet;
    private String donorInitData;
    private String authorWalletAddress = "EQDummyAuthorWalletAddress12345678901234567890"; // 48 символов
    private static final Long DONOR_USER_ID = 1001L;
    private static final Long AUTHOR_USER_ID = 1002L;

    @BeforeEach
    void setUp() {
        testSteps = new StickerSetTestSteps();
        testSteps.setMockMvc(mockMvc);
        testSteps.setObjectMapper(objectMapper);
        testSteps.setAppConfig(appConfig);
        testSteps.setStickerSetRepository(stickerSetRepository);
        testSteps.setUserRepository(userRepository);
        testSteps.setUserProfileRepository(userProfileRepository);

        // Создаем донатора
        testSteps.createTestUserAndProfile(DONOR_USER_ID);
        donorInitData = testSteps.createValidInitData(DONOR_USER_ID);

        // Создаем автора
        testSteps.createTestUserAndProfile(AUTHOR_USER_ID);
        authorUser = userRepository.findById(AUTHOR_USER_ID).orElseThrow();

        // Привязываем кошелёк автору
        walletService.linkWallet(authorUser.getId(), authorWalletAddress, null);

        // Создаем стикерсет
        testStickerSet = new StickerSet();
        testStickerSet.setUserId(authorUser.getId());
        testStickerSet.setAuthorId(authorUser.getId());
        testStickerSet.setTitle("Test StickerSet for Donation");
        testStickerSet.setName("test_donation_stickerset");
        stickerSetRepository.save(testStickerSet);
    }

    @Test
    @Story("Подготовка donation")
    @DisplayName("prepareTransaction должен создать donation intent для стикерсета с автором и кошельком")
    void prepareTransaction_shouldCreateDonationIntent() throws Exception {
        // Arrange
        PrepareTransactionRequest request = new PrepareTransactionRequest();
        request.setStickerSetId(testStickerSet.getId());
        request.setAmountNano(1_000_000_000L); // 1 TON

        // Act
        String responseJson = mockMvc.perform(post("/api/transactions/prepare")
                        .header("X-Telegram-Init-Data", donorInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(result -> {
                    System.out.println("🧪 Response Status: " + result.getResponse().getStatus());
                    System.out.println("🧪 Response Body: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intentId").exists())
                .andExpect(jsonPath("$.intentType").value("DONATION"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amountNano").value(1_000_000_000L))
                .andExpect(jsonPath("$.legs").isArray())
                .andExpect(jsonPath("$.legs[0].legType").value("MAIN"))
                .andExpect(jsonPath("$.legs[0].toWalletAddress").value(authorWalletAddress))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        PrepareTransactionResponse response = objectMapper.readValue(responseJson, PrepareTransactionResponse.class);
        assertThat(response.getIntentId()).isNotNull();
        assertThat(response.getLegs()).hasSize(1);
        assertThat(response.getLegs().get(0).getToWalletAddress()).isEqualTo(authorWalletAddress);
    }

    @Test
    @Story("Ошибки donation")
    @DisplayName("prepareTransaction должен вернуть 404 если стикерсет не найден")
    void prepareTransaction_shouldReturn404WhenStickerSetNotFound() throws Exception {
        // Arrange
        PrepareTransactionRequest request = new PrepareTransactionRequest();
        request.setStickerSetId(99999L); // Несуществующий ID
        request.setAmountNano(1_000_000_000L);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/prepare")
                        .header("X-Telegram-Init-Data", donorInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Story("Ошибки donation")
    @DisplayName("prepareTransaction должен вернуть 400 если у автора нет кошелька")
    void prepareTransaction_shouldReturn400WhenAuthorHasNoWallet() throws Exception {
        // Arrange - создаем стикерсет с автором без кошелька
        Long authorWithoutWalletId = 1003L;
        testSteps.createTestUserAndProfile(authorWithoutWalletId);
        UserEntity authorWithoutWallet = userRepository.findById(authorWithoutWalletId).orElseThrow();

        StickerSet stickerSetWithoutWallet = new StickerSet();
        stickerSetWithoutWallet.setUserId(authorWithoutWallet.getId());
        stickerSetWithoutWallet.setAuthorId(authorWithoutWallet.getId());
        stickerSetWithoutWallet.setTitle("StickerSet without wallet");
        stickerSetWithoutWallet.setName("test_no_wallet_stickerset");
        stickerSetRepository.save(stickerSetWithoutWallet);

        PrepareTransactionRequest request = new PrepareTransactionRequest();
        request.setStickerSetId(stickerSetWithoutWallet.getId());
        request.setAmountNano(1_000_000_000L);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/prepare")
                        .header("X-Telegram-Init-Data", donorInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Автор не привязал кошелёк"));
    }

    @Test
    @Story("Ошибки donation")
    @DisplayName("prepareTransaction должен вернуть 400 если стикерсет не имеет автора")
    void prepareTransaction_shouldReturn400WhenStickerSetHasNoAuthor() throws Exception {
        // Arrange - создаем стикерсет без автора
        StickerSet stickerSetWithoutAuthor = new StickerSet();
        stickerSetWithoutAuthor.setUserId(authorUser.getId());
        stickerSetWithoutAuthor.setAuthorId(null); // Нет автора
        stickerSetWithoutAuthor.setTitle("StickerSet without author");
        stickerSetWithoutAuthor.setName("test_no_author_stickerset");
        stickerSetRepository.save(stickerSetWithoutAuthor);

        PrepareTransactionRequest request = new PrepareTransactionRequest();
        request.setStickerSetId(stickerSetWithoutAuthor.getId());
        request.setAmountNano(1_000_000_000L);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/prepare")
                        .header("X-Telegram-Init-Data", donorInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("StickerSet не имеет автора"));
    }
}


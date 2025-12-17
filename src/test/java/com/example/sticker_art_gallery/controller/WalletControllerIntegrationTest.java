package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.wallet.LinkWalletRequest;
import com.example.sticker_art_gallery.model.transaction.UserWalletRepository;
import com.example.sticker_art_gallery.model.user.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для WalletController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("TON Wallets")
@Feature("Интеграция: Wallet API")
@DisplayName("Интеграционные тесты WalletController")
@org.junit.jupiter.api.Tag("integration")
@org.junit.jupiter.api.Tag("ton")
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private com.example.sticker_art_gallery.config.AppConfig appConfig;
    
    @Autowired
    private com.example.sticker_art_gallery.model.profile.UserProfileRepository userProfileRepository;

    private StickerSetTestSteps testSteps;
    private String initData;
    private static final Long TEST_USER_ID = 2001L;
    private static final String VALID_WALLET_ADDRESS = "EQDummyWalletAddress1234567890123456789012345678"; // 48 символов

    @BeforeEach
    void setUp() {
        testSteps = new StickerSetTestSteps();
        testSteps.setMockMvc(mockMvc);
        testSteps.setObjectMapper(objectMapper);
        testSteps.setAppConfig(appConfig);
        testSteps.setUserRepository(userRepository);
        testSteps.setUserProfileRepository(userProfileRepository);

        testSteps.createTestUserAndProfile(TEST_USER_ID);
        initData = testSteps.createValidInitData(TEST_USER_ID);
    }

    @Test
    @Story("Привязка кошелька")
    @DisplayName("linkWallet должен привязать кошелёк и деактивировать старые")
    void linkWallet_shouldLinkWalletAndDeactivateOld() throws Exception {
        // Arrange - создаем первый кошелёк
        String firstWallet = "EQFirstWalletAddress1234567890123456789012345678"; // 48 символов
        LinkWalletRequest firstRequest = new LinkWalletRequest();
        firstRequest.setWalletAddress(firstWallet);

        mockMvc.perform(post("/api/wallets/link")
                        .header("X-Telegram-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletAddress").value(firstWallet))
                .andExpect(jsonPath("$.isActive").value(true));

        // Act - привязываем второй кошелёк
        LinkWalletRequest secondRequest = new LinkWalletRequest();
        secondRequest.setWalletAddress(VALID_WALLET_ADDRESS);

        mockMvc.perform(post("/api/wallets/link")
                        .header("X-Telegram-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletAddress").value(VALID_WALLET_ADDRESS))
                .andExpect(jsonPath("$.isActive").value(true));

        // Assert - проверяем, что первый кошелёк деактивирован
        long activeWalletsCount = walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID).size();
        assertThat(activeWalletsCount).isEqualTo(1);
    }

    @Test
    @Story("Получение кошелька")
    @DisplayName("getMyWallet должен вернуть активный кошелёк")
    void getMyWallet_shouldReturnActiveWallet() throws Exception {
        // Arrange - привязываем кошелёк
        LinkWalletRequest request = new LinkWalletRequest();
        request.setWalletAddress(VALID_WALLET_ADDRESS);

        mockMvc.perform(post("/api/wallets/link")
                        .header("X-Telegram-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Act & Assert
        mockMvc.perform(get("/api/wallets/my")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletAddress").value(VALID_WALLET_ADDRESS))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @Story("Получение кошелька")
    @DisplayName("getMyWallet должен вернуть null если кошелёк не привязан")
    void getMyWallet_shouldReturnNullWhenNoWallet() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/wallets/my")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist()); // null в JSON
    }

    @Test
    @Story("Валидация")
    @DisplayName("linkWallet должен вернуть 400 для невалидного адреса")
    void linkWallet_shouldReturn400ForInvalidAddress() throws Exception {
        // Arrange
        LinkWalletRequest request = new LinkWalletRequest();
        request.setWalletAddress("invalid_address"); // Невалидный адрес

        // Act & Assert
        mockMvc.perform(post("/api/wallets/link")
                        .header("X-Telegram-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Story("Отвязывание кошелька")
    @DisplayName("unlinkWallet должен деактивировать кошелёк")
    void unlinkWallet_shouldDeactivateWallet() throws Exception {
        // Arrange - привязываем кошелёк
        LinkWalletRequest linkRequest = new LinkWalletRequest();
        linkRequest.setWalletAddress(VALID_WALLET_ADDRESS);

        mockMvc.perform(post("/api/wallets/link")
                        .header("X-Telegram-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk());

        // Проверяем, что кошелёк активен
        long activeWalletsBefore = walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID).size();
        assertThat(activeWalletsBefore).isEqualTo(1);

        // Act - отвязываем кошелёк
        mockMvc.perform(post("/api/wallets/unlink")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Assert - проверяем, что кошелёк деактивирован
        long activeWalletsAfter = walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID).size();
        assertThat(activeWalletsAfter).isEqualTo(0);
    }

    @Test
    @Story("Отвязывание кошелька")
    @DisplayName("unlinkWallet должен вернуть 200 когда кошелёк уже отвязан (идемпотентность)")
    void unlinkWallet_shouldReturn200WhenWalletAlreadyUnlinked() throws Exception {
        // Arrange - привязываем и сразу отвязываем кошелёк
        LinkWalletRequest linkRequest = new LinkWalletRequest();
        linkRequest.setWalletAddress(VALID_WALLET_ADDRESS);

        mockMvc.perform(post("/api/wallets/link")
                        .header("X-Telegram-Init-Data", initData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/wallets/unlink")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk());

        // Act - вызываем unlink второй раз
        mockMvc.perform(post("/api/wallets/unlink")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Assert - кошельков всё ещё нет
        long activeWallets = walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID).size();
        assertThat(activeWallets).isEqualTo(0);
    }

    @Test
    @Story("Отвязывание кошелька")
    @DisplayName("unlinkWallet должен вернуть 200 когда кошелёк не привязан")
    void unlinkWallet_shouldReturn200WhenNoWallet() throws Exception {
        // Act & Assert - вызываем unlink без привязки кошелька
        mockMvc.perform(post("/api/wallets/unlink")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Проверяем, что активных кошельков нет
        long activeWallets = walletRepository.findByUser_IdAndIsActiveTrue(TEST_USER_ID).size();
        assertThat(activeWallets).isEqualTo(0);
    }
}


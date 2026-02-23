package com.example.sticker_art_gallery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.sticker_art_gallery.testdata.TestUsers;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("ART-транзакции")
@Feature("Админ: создание ART-транзакции")
@DisplayName("ArtTransactionAdminController: POST создание транзакции")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArtTransactionAdminControllerIntegrationTest {

    private static final Long TARGET_USER_ID = TestUsers.OWNER.id();
    private static final Long ADMIN_USER_ID = TestUsers.ADMIN.id();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StickerSetTestSteps testSteps;
    @Autowired
    private ObjectMapper objectMapper;

    private String adminInitData;

    @BeforeAll
    void setUp() {
        testSteps.createTestUserAndProfile(TARGET_USER_ID);
        testSteps.createTestUserAndProfile(ADMIN_USER_ID);
        testSteps.makeAdmin(ADMIN_USER_ID);
        adminInitData = testSteps.createValidInitData(ADMIN_USER_ID);
    }

    @Test
    @Story("Создание транзакции")
    @DisplayName("POST с userId и amount создаёт CREDIT-транзакцию и возвращает 201")
    void createTransaction_credit_returns201AndTransaction() throws Exception {
        String body = "{\"userId\":" + TARGET_USER_ID + ",\"amount\":25}";
        mockMvc.perform(post("/api/admin/art-transactions")
                        .header("X-Telegram-Init-Data", adminInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction").exists())
                .andExpect(jsonPath("$.transaction.userId").value(TARGET_USER_ID))
                .andExpect(jsonPath("$.transaction.delta").value(25))
                .andExpect(jsonPath("$.transaction.ruleCode").value("ADMIN_MANUAL_CREDIT"))
                .andExpect(jsonPath("$.messageSent").exists());
    }

    @Test
    @Story("Валидация")
    @DisplayName("POST с amount=0 возвращает 400")
    void createTransaction_zeroAmount_returns400() throws Exception {
        String body = "{\"userId\":" + TARGET_USER_ID + ",\"amount\":0}";
        mockMvc.perform(post("/api/admin/art-transactions")
                        .header("X-Telegram-Init-Data", adminInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Story("Профиль пользователя")
    @DisplayName("PATCH профиля не меняет artBalance (баланс только через ART-транзакции)")
    void updateProfile_doesNotChangeArtBalance() throws Exception {
        String jsonBefore = mockMvc.perform(get("/api/users/" + TARGET_USER_ID + "/profile")
                        .header("X-Telegram-Init-Data", adminInitData))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long balanceBefore = objectMapper.readTree(jsonBefore).get("artBalance").asLong();

        mockMvc.perform(patch("/api/users/" + TARGET_USER_ID + "/profile")
                        .header("X-Telegram-Init-Data", adminInitData)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + TARGET_USER_ID + "/profile")
                        .header("X-Telegram-Init-Data", adminInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artBalance").value(balanceBefore));
    }
}

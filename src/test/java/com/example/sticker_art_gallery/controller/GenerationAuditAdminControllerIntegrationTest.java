package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Генерация стикеров")
@Feature("Админ: лог генерации")
@DisplayName("GenerationAuditAdminController: доступ и эндпоинты")
@Tag("integration")
class GenerationAuditAdminControllerIntegrationTest {

    private static final Long USER_ID = TestDataBuilder.TEST_USER_ID;
    private static final Long ADMIN_USER_ID = 999999002L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StickerSetTestSteps testSteps;

    private String userInitData;
    private String adminInitData;

    @BeforeAll
    void setUp() {
        testSteps.createTestUserAndProfile(USER_ID);
        testSteps.createTestUserAndProfile(ADMIN_USER_ID);
        testSteps.makeAdmin(ADMIN_USER_ID);
        userInitData = testSteps.createValidInitData(USER_ID);
        adminInitData = testSteps.createValidInitData(ADMIN_USER_ID);
    }

    @Test
    @Story("Права доступа")
    @DisplayName("USER получает 403 при запросе списка логов генерации")
    void userCannotAccessGenerationLogsList() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/admin/generation-logs")
                .header("X-Telegram-Init-Data", userInitData)
                .param("page", "0")
                .param("size", "20"));
        result.andExpect(status().isForbidden());
    }

    @Test
    @Story("Права доступа")
    @DisplayName("ADMIN получает 200 при запросе списка логов генерации")
    void adminCanAccessGenerationLogsList() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/admin/generation-logs")
                .header("X-Telegram-Init-Data", adminInitData)
                .param("page", "0")
                .param("size", "20"));
        result.andExpect(status().isOk());
    }

    @Test
    @Story("Детали по taskId")
    @DisplayName("ADMIN получает 404 для несуществующего taskId")
    void adminGetDetailNotFound() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/admin/generation-logs/non-existent-task-id-12345")
                .header("X-Telegram-Init-Data", adminInitData));
        result.andExpect(status().isNotFound());
    }

    @Test
    @Story("События по taskId")
    @DisplayName("ADMIN получает 200 для событий несуществующего taskId (пустой список)")
    void adminGetEventsReturnsOk() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/admin/generation-logs/non-existent-task-id-12345/events")
                .header("X-Telegram-Init-Data", adminInitData));
        result.andExpect(status().isOk());
    }
}

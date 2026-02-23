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
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Аналитика")
@Feature("Админ: дашборд аналитики")
@DisplayName("AnalyticsAdminController: доступ и контракт дашборда")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsAdminControllerIntegrationTest {

    private static final Long USER_ID = TestDataBuilder.TEST_USER_ID;
    private static final Long ADMIN_USER_ID = 999999003L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StickerSetTestSteps testSteps;

    private String userInitData;
    private String adminInitData;

    private static String toIso(OffsetDateTime dt) {
        return dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

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
    @DisplayName("USER получает 403 при запросе дашборда аналитики")
    void userCannotAccessDashboard() throws Exception {
        OffsetDateTime to = OffsetDateTime.now();
        OffsetDateTime from = to.minusDays(7);
        ResultActions result = mockMvc.perform(get("/api/admin/analytics/dashboard")
                .header("X-Telegram-Init-Data", userInitData)
                .param("from", toIso(from))
                .param("to", toIso(to))
                .param("granularity", "day"));
        result.andExpect(status().isForbidden());
    }

    @Test
    @Story("Права доступа")
    @DisplayName("ADMIN получает 200 и тело с kpiCards, timeseries, breakdowns")
    void adminCanAccessDashboard() throws Exception {
        OffsetDateTime to = OffsetDateTime.now();
        OffsetDateTime from = to.minusDays(7);
        ResultActions result = mockMvc.perform(get("/api/admin/analytics/dashboard")
                .header("X-Telegram-Init-Data", adminInitData)
                .param("from", toIso(from))
                .param("to", toIso(to))
                .param("granularity", "day"));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.from").exists())
                .andExpect(jsonPath("$.to").exists())
                .andExpect(jsonPath("$.granularity").value("day"))
                .andExpect(jsonPath("$.kpiCards").exists())
                .andExpect(jsonPath("$.kpiCards.totalUsers").isNumber())
                .andExpect(jsonPath("$.timeseries").exists())
                .andExpect(jsonPath("$.breakdowns").exists());
    }

    @Test
    @Story("Валидация")
    @DisplayName("Невалидный диапазон (from > to) возвращает 400")
    void invalidRangeReturns400() throws Exception {
        OffsetDateTime to = OffsetDateTime.now().minusDays(10);
        OffsetDateTime from = OffsetDateTime.now().minusDays(2);
        ResultActions result = mockMvc.perform(get("/api/admin/analytics/dashboard")
                .header("X-Telegram-Init-Data", adminInitData)
                .param("from", toIso(from))
                .param("to", toIso(to))
                .param("granularity", "day"));
        result.andExpect(status().isBadRequest());
    }
}

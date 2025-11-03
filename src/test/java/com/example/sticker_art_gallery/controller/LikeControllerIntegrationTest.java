package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("API для лайков")
@Feature("Интеграция: лайки и сортировка по likesCount")
@DisplayName("Интеграционные тесты LikeController")
@Tag("integration")
class LikeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private StickerSetRepository stickerSetRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    @Story("likesCount попадает в ответы и влияет на сортировку")
    void likesCountShouldBeReturnedAndAffectSorting() throws Exception {
        StickerSetTestSteps steps = new StickerSetTestSteps();
        steps.setMockMvc(mockMvc);
        steps.setObjectMapper(objectMapper);
        steps.setAppConfig(appConfig);
        steps.setStickerSetRepository(stickerSetRepository);
        steps.setUserRepository(userRepository);
        steps.setUserProfileRepository(userProfileRepository);
        steps.cleanupTestData();
        steps.createTestUserAndProfile(TestDataBuilder.TEST_USER_ID);
        String initData = steps.createValidInitData(TestDataBuilder.TEST_USER_ID);

        // Создаём валидные стикерсеты через API (валидируются через Telegram)
        var res1 = steps.createStickerSet(
                com.example.sticker_art_gallery.testdata.TestDataBuilder.createStickerSetDtoWithUrl("Dunem"), initData)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id1 = objectMapper.readTree(res1).get("id").asLong();

        var res2 = steps.createStickerSet(
                com.example.sticker_art_gallery.testdata.TestDataBuilder.createStickerSetDtoWithUrl("citati_prosto"), initData)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id2 = objectMapper.readTree(res2).get("id").asLong();

        // Ставим один лайк первому
        mockMvc.perform(post("/api/likes/stickersets/" + id1)
                .header("X-Telegram-Init-Data", initData)
                .header("X-Telegram-Bot-Name", com.example.sticker_art_gallery.testdata.TestDataBuilder.BOT_NAME)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        // Проверяем, что likesCount=1 у первого, 0 у второго через GET by id
        String s1 = mockMvc.perform(get("/api/stickersets/" + id1))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode js1 = objectMapper.readTree(s1);
        assertThat(js1.get("likesCount").asInt()).isEqualTo(1);

        String s2 = mockMvc.perform(get("/api/stickersets/" + id2))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode js2 = objectMapper.readTree(s2);
        assertThat(js2.get("likesCount").asInt()).isEqualTo(0);

        // Уберём лайк и проверим, что счётчик и порядок меняются
        mockMvc.perform(delete("/api/likes/stickersets/" + id1)
                .header("X-Telegram-Init-Data", initData)
                .header("X-Telegram-Bot-Name", com.example.sticker_art_gallery.testdata.TestDataBuilder.BOT_NAME))
            .andExpect(status().isOk());

        String s1after = mockMvc.perform(get("/api/stickersets/" + id1))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(s1after).get("likesCount").asInt()).isEqualTo(0);
    }
}



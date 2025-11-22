package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("Стикерсеты")
@Feature("Режим превью")
@DisplayName("Интеграция: режим превью для списков стикерсетов")
@Tag("integration")
class StickerSetPreviewModeIntegrationTest {

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    @MockBean
    private TelegramBotApiService telegramBotApiService;

    private String initData;
    private final Long userId = TestDataBuilder.TEST_USER_ID;
    private static final String STICKERSET_NAME = "test_preview_pack";

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(userId);
        initData = testSteps.createValidInitData(userId);

        stickerSetRepository.deleteAll();

        // Создаем тестовый стикерсет
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle("Test Preview Pack");
        stickerSet.setName(STICKERSET_NAME);
        stickerSet.setState(StickerSetState.ACTIVE);
        stickerSet.setVisibility(StickerSetVisibility.PUBLIC);
        stickerSet.setType(StickerSetType.USER);
        stickerSetRepository.save(stickerSet);

        // Мокируем TelegramBotApiService для возврата объекта с 10 стикерами
        Map<String, Object> telegramInfo = createMockTelegramStickerSetInfoWithStickers(10);
        when(telegramBotApiService.getStickerSetInfo(STICKERSET_NAME)).thenReturn(telegramInfo);
    }

    @AfterEach
    void tearDown() {
        stickerSetRepository.deleteAll();
    }

    @Test
    @Story("Preview mode")
    @DisplayName("preview=true возвращает только 3 случайных стикера")
    @Severity(SeverityLevel.NORMAL)
    void previewMode_ShouldReturnOnlyThreeStickers() throws Exception {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("preview", "true");
        testSteps.getAllStickerSets(initData, params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.stickers").isArray())
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.stickers.length()").value(3))
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.name").value(STICKERSET_NAME))
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.title").value("Test Preview Pack"));
    }

    @Test
    @Story("Preview mode")
    @DisplayName("preview=false возвращает все стикеры")
    @Severity(SeverityLevel.NORMAL)
    void previewModeFalse_ShouldReturnAllStickers() throws Exception {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("preview", "false");
        testSteps.getAllStickerSets(initData, params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.stickers").isArray())
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.stickers.length()").value(10));
    }

    @Test
    @Story("Preview mode")
    @DisplayName("preview mode работает для поиска стикерсетов")
    @Severity(SeverityLevel.NORMAL)
    void previewMode_ShouldWorkForSearch() throws Exception {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("preview", "true");
        testSteps.getStickerSetsWithFilters(null, null, null, initData, params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].telegramStickerSetInfo.stickers.length()").value(3));
    }

    /**
     * Создает мок объект telegramStickerSetInfo с указанным количеством стикеров
     */
    private Map<String, Object> createMockTelegramStickerSetInfoWithStickers(int stickerCount) {
        Map<String, Object> telegramInfo = new LinkedHashMap<>();
        telegramInfo.put("name", STICKERSET_NAME);
        telegramInfo.put("title", "Test Preview Pack");
        telegramInfo.put("sticker_type", "regular");
        telegramInfo.put("contains_masks", false);

        List<Map<String, Object>> stickers = new ArrayList<>();
        for (int i = 0; i < stickerCount; i++) {
            Map<String, Object> sticker = new LinkedHashMap<>();
            sticker.put("file_id", "sticker_" + i);
            sticker.put("file_unique_id", "unique_" + i);
            sticker.put("width", 512);
            sticker.put("height", 512);
            sticker.put("emoji", "😀");
            stickers.add(sticker);
        }

        telegramInfo.put("stickers", stickers);
        return telegramInfo;
    }
}


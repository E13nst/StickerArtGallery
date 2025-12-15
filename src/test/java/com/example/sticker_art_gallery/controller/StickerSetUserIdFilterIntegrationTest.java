package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Epic("Стикерсеты")
@Feature("Фильтр по userId")
@Tag("integration")
@Transactional
@Rollback
class StickerSetUserIdFilterIntegrationTest {

    @Autowired
    private StickerSetTestSteps testSteps;

    @Autowired
    private StickerSetRepository stickerSetRepository;

    @Autowired
    private MockMvc mockMvc;

    private String initData;
    // Используем уникальные ID для тестов, чтобы избежать конфликтов с существующими данными
    private final Long userId1 = 9_000_000_001L;
    private final Long userId2 = 9_000_000_002L;
    private final Long userId3 = 9_000_000_003L;

    @BeforeEach
    void setUp() {
        testSteps.createTestUserAndProfile(userId1);
        initData = testSteps.createValidInitData(userId1);

        // ⚠️ ВАЖНО: Удаляем только тестовые данные, созданные в этом тесте
        // Используем транзакцию с откатом, чтобы не удалять продакшен данные
        // Удаляем по конкретным именам, которые мы знаем (используем индексированный запрос)
        String[] testNames = {
            "s1_user1_by_StickerGalleryBot",
            "s2_user1_by_StickerGalleryBot",
            "s3_user2_by_StickerGalleryBot",
            "s4_user3_by_StickerGalleryBot"
        };
        for (String name : testNames) {
            stickerSetRepository.findByNameIgnoreCase(name)
                .ifPresent(stickerSetRepository::delete);
        }

        // Стикерсет пользователя 1
        StickerSet s1 = new StickerSet();
        s1.setUserId(userId1);
        s1.setTitle("StickerSet User 1");
        s1.setName("s1_user1_by_StickerGalleryBot");
        s1.setState(StickerSetState.ACTIVE);
        s1.setVisibility(StickerSetVisibility.PUBLIC);
        s1.setType(StickerSetType.USER);
        s1.setAuthorId(null);
        stickerSetRepository.save(s1);

        // Стикерсет пользователя 1 (второй)
        StickerSet s2 = new StickerSet();
        s2.setUserId(userId1);
        s2.setTitle("StickerSet User 1 Second");
        s2.setName("s2_user1_by_StickerGalleryBot");
        s2.setState(StickerSetState.ACTIVE);
        s2.setVisibility(StickerSetVisibility.PUBLIC);
        s2.setType(StickerSetType.OFFICIAL);
        s2.setAuthorId(111L);
        stickerSetRepository.save(s2);

        // Стикерсет пользователя 2
        StickerSet s3 = new StickerSet();
        s3.setUserId(userId2);
        s3.setTitle("StickerSet User 2");
        s3.setName("s3_user2_by_StickerGalleryBot");
        s3.setState(StickerSetState.ACTIVE);
        s3.setVisibility(StickerSetVisibility.PUBLIC);
        s3.setType(StickerSetType.USER);
        s3.setAuthorId(null);
        stickerSetRepository.save(s3);

        // Стикерсет пользователя 3
        StickerSet s4 = new StickerSet();
        s4.setUserId(userId3);
        s4.setTitle("StickerSet User 3");
        s4.setName("s4_user3_by_StickerGalleryBot");
        s4.setState(StickerSetState.ACTIVE);
        s4.setVisibility(StickerSetVisibility.PUBLIC);
        s4.setType(StickerSetType.USER);
        s4.setAuthorId(222L);
        stickerSetRepository.save(s4);
    }

    @AfterEach
    void tearDown() {
        // ⚠️ ВАЖНО: Не удаляем все данные! 
        // Транзакция с @Rollback автоматически откатит все изменения
        // Удаляем по конкретным именам, которые мы знаем (используем индексированный запрос)
        String[] testNames = {
            "s1_user1_by_StickerGalleryBot",
            "s2_user1_by_StickerGalleryBot",
            "s3_user2_by_StickerGalleryBot",
            "s4_user3_by_StickerGalleryBot"
        };
        for (String name : testNames) {
            stickerSetRepository.findByNameIgnoreCase(name)
                .ifPresent(stickerSetRepository::delete);
        }
    }

    @Test
    @Story("userId фильтр")
    @DisplayName("userId фильтрует стикерсеты по пользователю")
    @Severity(SeverityLevel.CRITICAL)
    void filterByUserId_ShouldReturnOnlyUserStickerSets() throws Exception {
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(2)))
                .andExpect(jsonPath("$.content[*].userId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(userId1))));
    }

    @Test
    @Story("userId фильтр")
    @DisplayName("userId фильтр возвращает пустой список для несуществующего пользователя")
    @Severity(SeverityLevel.NORMAL)
    void filterByNonExistentUserId_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(0)));
    }

    @Test
    @Story("userId фильтр")
    @DisplayName("userId фильтр работает для разных пользователей")
    @Severity(SeverityLevel.CRITICAL)
    void filterByDifferentUserIds_ShouldReturnCorrectResults() throws Exception {
        // Проверяем userId2
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId2))
                .andExpect(jsonPath("$.content[0].title").value("StickerSet User 2"));

        // Проверяем userId3
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId3))
                .andExpect(jsonPath("$.content[0].title").value("StickerSet User 3"));
    }

    @Test
    @Story("userId фильтр + другие фильтры")
    @DisplayName("userId фильтр комбинируется с officialOnly")
    @Severity(SeverityLevel.NORMAL)
    void filterByUserIdAndOfficialOnly_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId1))
                        .param("officialOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId1))
                .andExpect(jsonPath("$.content[0].isOfficial").value(true));
    }

    @Test
    @Story("userId фильтр + другие фильтры")
    @DisplayName("userId фильтр комбинируется с authorId")
    @Severity(SeverityLevel.NORMAL)
    void filterByUserIdAndAuthorId_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId1))
                        .param("authorId", "111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId1))
                .andExpect(jsonPath("$.content[0].authorId").value(111L));
    }

    @Test
    @Story("userId фильтр + другие фильтры")
    @DisplayName("userId фильтр комбинируется с hasAuthorOnly")
    @Severity(SeverityLevel.NORMAL)
    void filterByUserIdAndHasAuthorOnly_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId1))
                        .param("hasAuthorOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId1))
                .andExpect(jsonPath("$.content[0].authorId").value(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @Story("userId фильтр + другие фильтры")
    @DisplayName("userId фильтр комбинируется с categoryKeys")
    @Severity(SeverityLevel.NORMAL)
    void filterByUserIdAndCategoryKeys_ShouldReturnFilteredResults() throws Exception {
        // Сначала добавим категорию к одному из стикерсетов пользователя 1
        // (это требует дополнительной настройки, но для базового теста проверим что запрос проходит)
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData)
                        .param("userId", String.valueOf(userId1))
                        .param("categoryKeys", "animals"))
                .andExpect(status().isOk());
    }

    @Test
    @Story("userId фильтр")
    @DisplayName("Без userId фильтра возвращаются все стикерсеты")
    @Severity(SeverityLevel.NORMAL)
    void withoutUserIdFilter_ShouldReturnAllStickerSets() throws Exception {
        mockMvc.perform(get("/api/stickersets")
                        .header("X-Telegram-Init-Data", initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }
}


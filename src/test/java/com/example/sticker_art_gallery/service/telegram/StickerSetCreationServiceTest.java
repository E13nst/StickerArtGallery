package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Создание стикерсетов")
@Feature("StickerSetCreationService")
@DisplayName("Тесты StickerSetCreationService")
class StickerSetCreationServiceTest {

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private TelegramBotApiService telegramBotApiService;

    @Mock
    private StickerSetService stickerSetService;

    @Mock
    private StickerSetTelegramCacheService stickerSetTelegramCacheService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.Telegram telegramConfig;

    private StickerSetNamingService namingService;
    private StickerSetCreationService creationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        namingService = new StickerSetNamingService("stixlybot");
        creationService = new StickerSetCreationService(
                imageStorageService,
                telegramBotApiService,
                stickerSetService,
                namingService,
                stickerSetTelegramCacheService,
                userRepository,
                appConfig
        );
        when(appConfig.getTelegram()).thenReturn(telegramConfig);
        when(telegramConfig.getDefaultStickerSetTitle()).thenReturn("Test Title");
    }

    @Test
    @Story("Автодобавление суффикса имени")
    @DisplayName("createWithSticker с именем без суффикса передаёт в Telegram API имя с _by_stixlybot")
    @Description("При name=spamsticks вызов createNewStickerSet получает spamsticks_by_stixlybot")
    @Severity(SeverityLevel.CRITICAL)
    void createWithSticker_WithNameWithoutSuffix_CallsTelegramApiWithSuffixedName() throws Exception {
        UUID imageUuid = UUID.randomUUID();
        File stickerFile = tempDir.resolve("sticker.png").toFile();
        stickerFile.createNewFile();
        when(imageStorageService.getFileByUuid(imageUuid)).thenReturn(stickerFile);
        when(telegramBotApiService.createNewStickerSet(anyLong(), any(File.class), anyString(), anyString(), anyString()))
                .thenReturn(true);

        StickerSet saved = new StickerSet();
        saved.setId(1L);
        saved.setName("spamsticks_by_stixlybot");
        when(stickerSetService.createStickerSetForUser(any(), any(Long.class), anyString(), any(Long.class)))
                .thenReturn(saved);

        creationService.createWithSticker(
                100L,
                imageUuid,
                "My Title",
                "spamsticks",
                "🎨",
                Set.of(),
                StickerSetVisibility.PRIVATE
        );

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramBotApiService).createNewStickerSet(
                eq(100L),
                eq(stickerFile),
                nameCaptor.capture(),
                eq("My Title"),
                eq("🎨")
        );
        assertEquals("spamsticks_by_stixlybot", nameCaptor.getValue());
    }

    @Test
    @Story("Автодобавление суффикса имени")
    @DisplayName("createWithSticker с именем уже с суффиксом не дублирует суффикс")
    void createWithSticker_WithNameAlreadySuffixed_KeepsNameUnchanged() throws Exception {
        UUID imageUuid = UUID.randomUUID();
        File stickerFile = tempDir.resolve("sticker2.png").toFile();
        stickerFile.createNewFile();
        when(imageStorageService.getFileByUuid(imageUuid)).thenReturn(stickerFile);
        when(telegramBotApiService.createNewStickerSet(anyLong(), any(File.class), anyString(), anyString(), anyString()))
                .thenReturn(true);

        StickerSet saved = new StickerSet();
        saved.setId(2L);
        saved.setName("mypack_by_stixlybot");
        when(stickerSetService.createStickerSetForUser(any(), any(Long.class), anyString(), any(Long.class)))
                .thenReturn(saved);

        creationService.createWithSticker(
                200L,
                imageUuid,
                "Title",
                "mypack_by_stixlybot",
                null,
                null,
                null
        );

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramBotApiService).createNewStickerSet(
                eq(200L),
                eq(stickerFile),
                nameCaptor.capture(),
                eq("Title"),
                eq("🎨")
        );
        assertEquals("mypack_by_stixlybot", nameCaptor.getValue());
    }
}

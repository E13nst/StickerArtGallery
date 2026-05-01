package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.SaveImageToStickerSetResponseDto;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
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
import java.util.Optional;
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
        when(stickerSetService.createStickerSetForUser(
                any(),
                anyLong(),
                anyString(),
                anyBoolean(),
                any(StickerSetType.class)))
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
        verify(stickerSetService).createStickerSetForUser(
                any(),
                eq(100L),
                anyString(),
                eq(true),
                eq(StickerSetType.GENERATED)
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
        when(stickerSetService.createStickerSetForUser(
                any(),
                anyLong(),
                anyString(),
                anyBoolean(),
                any(StickerSetType.class)))
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
        verify(stickerSetService).createStickerSetForUser(
                any(),
                eq(200L),
                anyString(),
                eq(true),
                eq(StickerSetType.GENERATED)
        );
        assertEquals("mypack_by_stixlybot", nameCaptor.getValue());
    }

    @Test
    @DisplayName("createWithSticker при занятом имени ретраит с новым именем")
    void createWithSticker_WhenNameOccupied_RetriesWithGeneratedSuffix() throws Exception {
        UUID imageUuid = UUID.randomUUID();
        File stickerFile = tempDir.resolve("sticker3.png").toFile();
        stickerFile.createNewFile();
        when(imageStorageService.getFileByUuid(imageUuid)).thenReturn(stickerFile);

        when(telegramBotApiService.createNewStickerSet(anyLong(), any(File.class), anyString(), anyString(), anyString()))
                .thenReturn(false)
                .thenReturn(true);

        StickerSet saved = new StickerSet();
        saved.setId(3L);
        saved.setName("retrypack_g1_by_stixlybot");
        when(stickerSetService.createStickerSetForUser(
                any(),
                anyLong(),
                anyString(),
                anyBoolean(),
                any(StickerSetType.class)))
                .thenReturn(saved);

        creationService.createWithSticker(
                300L,
                imageUuid,
                "Retry Title",
                "retrypack",
                "🎨",
                Set.of(),
                StickerSetVisibility.PRIVATE
        );

        verify(telegramBotApiService).createNewStickerSet(
                eq(300L),
                eq(stickerFile),
                eq("retrypack_by_stixlybot"),
                eq("Retry Title"),
                eq("🎨")
        );
        verify(telegramBotApiService).createNewStickerSet(
                eq(300L),
                eq(stickerFile),
                eq("retrypack_g1_by_stixlybot"),
                eq("Retry Title"),
                eq("🎨")
        );

        ArgumentCaptor<com.example.sticker_art_gallery.dto.CreateStickerSetDto> dtoCaptor =
                ArgumentCaptor.forClass(com.example.sticker_art_gallery.dto.CreateStickerSetDto.class);
        verify(stickerSetService).createStickerSetForUser(
                dtoCaptor.capture(),
                eq(300L),
                anyString(),
                eq(true),
                eq(StickerSetType.GENERATED)
        );
        assertEquals("retrypack_g1_by_stixlybot", dtoCaptor.getValue().getName());
    }

    @Test
    @DisplayName("saveImageToStickerSet: первое создание дефолтного набора вызывает ensureTelegramStickerSetInGallery")
    void saveImageToStickerSet_whenDefaultSetCreated_ensuresGalleryRegistration() throws Exception {
        long userId = 400L;
        UUID imageUuid = UUID.randomUUID();
        File stickerFile = tempDir.resolve("default-new.png").toFile();
        assertTrue(stickerFile.createNewFile());
        when(imageStorageService.getFileByUuid(imageUuid)).thenReturn(stickerFile);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("alice");
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(appConfig.getTelegram()).thenReturn(telegramConfig);
        when(telegramConfig.getDefaultStickerSetTitle()).thenReturn("Gallery Default");

        String expectedName = "alice_by_stixlybot";
        when(telegramBotApiService.getStickerSetInfoSimple(expectedName)).thenReturn(null);
        when(telegramBotApiService.createNewStickerSet(
                eq(userId),
                eq(stickerFile),
                eq(expectedName),
                eq("Gallery Default"),
                eq("🎨"))).thenReturn(true);
        when(telegramBotApiService.getStickerFileId(expectedName, 0)).thenReturn("tg-file-1");

        SaveImageToStickerSetResponseDto result =
                creationService.saveImageToStickerSet(userId, imageUuid, null, "🎨");

        assertEquals(expectedName, result.getStickerSetName());
        verify(stickerSetService).ensureTelegramStickerSetInGallery(
                eq(userId), eq(expectedName), eq("Gallery Default"), eq(StickerSetType.GENERATED), eq(true));
    }
}

package com.example.sticker_art_gallery.service.telegram;

import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Бизнес-логика стикерсетов")
@Feature("Имена стикерсетов")
@DisplayName("Тесты StickerSetNamingService")
class StickerSetNamingServiceTest {

    private StickerSetNamingService namingService;

    @BeforeEach
    void setUp() {
        namingService = new StickerSetNamingService("stixlybot");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @Story("ensureBotSuffix")
    @DisplayName("ensureBotSuffix с null/blank возвращает без изменений")
    @Description("null и пустые строки не изменяются")
    @Severity(SeverityLevel.CRITICAL)
    void ensureBotSuffix_WithNullOrBlank_ReturnsUnchanged(String input) {
        assertSame(input, namingService.ensureBotSuffix(input));
    }

    @Test
    @Story("ensureBotSuffix")
    @DisplayName("ensureBotSuffix без суффикса добавляет _by_stixlybot")
    @Description("spamsticks -> spamsticks_by_stixlybot")
    @Severity(SeverityLevel.CRITICAL)
    void ensureBotSuffix_WithoutSuffix_AddsSuffix() {
        assertEquals("spamsticks_by_stixlybot", namingService.ensureBotSuffix("spamsticks"));
        assertEquals("mystickers_by_stixlybot", namingService.ensureBotSuffix("MyStickers"));
        assertEquals("test123_by_stixlybot", namingService.ensureBotSuffix("  test123  "));
    }

    @Test
    @Story("ensureBotSuffix")
    @DisplayName("ensureBotSuffix с уже имеющимся суффиксом не дублирует")
    @Description("name_by_stixlybot остаётся без изменений (lowercase)")
    @Severity(SeverityLevel.CRITICAL)
    void ensureBotSuffix_WithExistingSuffix_ReturnsNormalizedUnchanged() {
        assertEquals("spamsticks_by_stixlybot", namingService.ensureBotSuffix("spamsticks_by_stixlybot"));
        assertEquals("mystickers_by_stixlybot", namingService.ensureBotSuffix("MyStickers_by_Stixlybot"));
        assertEquals("user_123_by_stixlybot", namingService.ensureBotSuffix("user_123_by_stixlybot"));
    }

    @Test
    @Story("ensureBotSuffix")
    @DisplayName("ensureBotSuffix тримит и приводит к нижнему регистру")
    void ensureBotSuffix_TrimsAndLowercases() {
        assertEquals("abc_by_stixlybot", namingService.ensureBotSuffix("  ABC  "));
    }

    @Test
    @Story("generateDefaultName")
    @DisplayName("generateDefaultName создаёт имя с суффиксом")
    void generateDefaultName_ProducesNameWithSuffix() {
        String name = namingService.generateDefaultName(12345L, "johndoe");
        assertEquals("johndoe_by_stixlybot", name);
        assertTrue(namingService.isOwnedByBot(name));
    }

    @Test
    @Story("isOwnedByBot")
    @DisplayName("isOwnedByBot возвращает true для имени с суффиксом stixlybot")
    void isOwnedByBot_WithCorrectSuffix_ReturnsTrue() {
        assertTrue(namingService.isOwnedByBot("spamsticks_by_stixlybot"));
        assertTrue(namingService.isOwnedByBot("any_by_stixlybot"));
    }

    @Test
    @Story("isOwnedByBot")
    @DisplayName("isOwnedByBot возвращает false для имени без суффикса")
    void isOwnedByBot_WithoutSuffix_ReturnsFalse() {
        assertFalse(namingService.isOwnedByBot("spamsticks"));
        assertFalse(namingService.isOwnedByBot("other_by_anotherbot"));
    }
}

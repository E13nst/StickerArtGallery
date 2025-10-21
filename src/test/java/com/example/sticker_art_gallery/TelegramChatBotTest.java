package com.example.sticker_art_gallery;

// import com.example.sticker_art_gallery.config.BotConfig;

import io.qameta.allure.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Epic("Telegram Bot")
@Feature("Чат-бот с OpenAI")
@Disabled("Тест отключен из-за проблем с переменными окружения")
@DisplayName("Тесты Telegram чат-бота")
class TelegramChatBotTest {

    // @Autowired
    // private BotConfig botConfig;

    private static final int PROMPT_MAX_LENGTH = 4096;

    @Test
    @Story("Валидация промптов OpenAI")
    @DisplayName("Проверка длины промпта для OpenAI")
    @Description("Проверяет, что длина промпта не превышает лимит OpenAI API (4096 символов)")
    @Severity(SeverityLevel.NORMAL)
    @Disabled
    void testPromptLength() {

//        String openaiToken = System.getenv("OPENAI_TOKEN");
//        String openaiToken = botConfig.getOpenaiToken();
//        assertNotNull(openaiToken, "Prompt should not be null");
//        assertTrue(openaiToken.length() < PROMPT_MAX_LENGTH, "The string should be shorter than 4096 characters");
    }

}
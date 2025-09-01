package com.example.dream_stream_bot;

// import com.example.dream_stream_bot.config.BotConfig;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Тест отключен из-за проблем с переменными окружения")
class TelegramChatBotTest {

    // @Autowired
    // private BotConfig botConfig;

    private static final int PROMPT_MAX_LENGTH = 4096;

    @Test
    @DisplayName("Prompt length")
    @Disabled
    void testPromptLength() {

//        String openaiToken = System.getenv("OPENAI_TOKEN");
//        String openaiToken = botConfig.getOpenaiToken();
//        assertNotNull(openaiToken, "Prompt should not be null");
//        assertTrue(openaiToken.length() < PROMPT_MAX_LENGTH, "The string should be shorter than 4096 characters");
    }

}
package com.example.sticker_art_gallery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Компонент для валидации обязательных переменных окружения при старте приложения
 */
@Component
public class EnvironmentValidationConfig implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidationConfig.class);

    private final Environment environment;

    @Autowired
    public EnvironmentValidationConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("🔍 ===== НАЧИНАЕМ ВАЛИДАЦИЮ ПЕРЕМЕННЫХ ОКРУЖЕНИЯ =====");
        
        List<String> missingVariables = new ArrayList<>();
        
        // Проверяем обязательные переменные
        validateRequiredVariable("TELEGRAM_BOT_TOKEN", missingVariables);
        validateRequiredVariable("DB_HOST", missingVariables);
        validateRequiredVariable("DB_NAME", missingVariables);
        validateRequiredVariable("DB_USERNAME", missingVariables);
        validateRequiredVariable("DB_PASSWORD", missingVariables);
        validateRequiredVariable("APP_URL", missingVariables);
        validateRequiredVariable("STICKER_PROCESSOR_URL", missingVariables);
        
        if (!missingVariables.isEmpty()) {
            LOGGER.error("❌ ===== ОБНАРУЖЕНЫ ОТСУТСТВУЮЩИЕ ПЕРЕМЕННЫЕ =====");
            for (String variable : missingVariables) {
                LOGGER.error("   - {}", variable);
            }
            
            LOGGER.error("❌ Приложение не может быть запущено без этих переменных!");
            LOGGER.error("❌ Пожалуйста, проверьте файл .env.app или переменные окружения в продакшене");
            
            throw new IllegalStateException("Отсутствуют обязательные переменные окружения: " + String.join(", ", missingVariables));
        }
        
        LOGGER.info("✅ ===== ВСЕ ПЕРЕМЕННЫЕ НАСТРОЕНЫ КОРРЕКТНО =====");
        LOGGER.info("🚀 Приложение готово к работе");
    }

    private void validateRequiredVariable(String variableName, List<String> missingVariables) {
        String value = environment.getProperty(variableName);
        if (value == null || value.trim().isEmpty()) {
            missingVariables.add(variableName);
            LOGGER.warn("⚠️ Переменная окружения '{}' не определена или пуста", variableName);
        } else {
            LOGGER.debug("✅ Переменная '{}' определена", variableName);
        }
    }

}

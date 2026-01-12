package com.example.sticker_art_gallery.service.ai;

import com.example.sticker_art_gallery.dto.*;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.repository.CategoryRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Сервис для автоматической категоризации стикерсетов с помощью AI
 */
@Service
public class AutoCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(AutoCategorizationService.class);
    private static final Pattern CATEGORY_KEY_PATTERN = Pattern.compile("^[a-z_]+$");
    private static final String SYSTEM_PROMPT_PATH = "ai/prompts/auto_category.system.txt";
    private static final String USER_PROMPT_PATH = "ai/prompts/auto_category.user.txt";

    private final AIService aiService;
    private final StickerSetService stickerSetService;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.auto-category.max-categories:5}")
    private int maxCategories;

    @Autowired
    public AutoCategorizationService(
            AIService aiService,
            StickerSetService stickerSetService,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.stickerSetService = stickerSetService;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * МЕТОД #1: Предлагает категории для title без обращения к БД стикерсетов
     * 
     * @param title заголовок стикерсета для анализа
     * @param language язык для локализации названий категорий
     * @return результат с предложенными категориями и уровнем уверенности
     */
    // TODO: Настроить кеш categorySuggestions в конфигурации
    // @Cacheable(value = "categorySuggestions", key = "#title + '_' + #language")
    public CategorySuggestionResult suggestCategoriesForTitle(String title, String language) {
        log.info("🤖 Предложение категорий для title: '{}'", title);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title не может быть пустым");
        }

        // Получаем список активных категорий
        List<Category> availableCategories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        if (availableCategories.isEmpty()) {
            log.warn("⚠️ Нет активных категорий в системе");
            return new CategorySuggestionResult(title, Collections.emptyList(), "No active categories available");
        }

        String categoriesList = formatCategoriesList(availableCategories);
        log.debug("📁 Доступно категорий: {}", availableCategories.size());

        // Загружаем промпты
        String systemPrompt = loadPrompt(SYSTEM_PROMPT_PATH);
        String userPromptTemplate = loadPrompt(USER_PROMPT_PATH);

        // Формируем промпты с подстановкой значений
        String systemPromptWithValues = systemPrompt.replace("${maxCategories}", String.valueOf(maxCategories));
        String userPrompt = userPromptTemplate
                .replace("${title}", title)
                .replace("${availableCategories}", categoriesList)
                .replace("${maxCategories}", String.valueOf(maxCategories));

        // Безопасность: избегаем незамененных плейсхолдеров шаблонизатора (${} -> $ {}),
        // но сохраняем фигурные скобки для JSON примеров в промптах
        systemPromptWithValues = systemPromptWithValues.replace("${", "$ {");
        userPrompt = userPrompt.replace("${", "$ {");

        // Вызываем AI
        log.debug("🤖 Отправка запроса к AI для категоризации");
        String aiResponse = callAiWithoutTemplate("category-suggestion-" + title.hashCode(), systemPromptWithValues, userPrompt);

        log.debug("🤖 Ответ AI: {}", aiResponse);

        // Парсим JSON ответ
        CategorySuggestionDto suggestion = parseAIResponse(aiResponse);
        if (suggestion == null || suggestion.getCategories() == null || suggestion.getCategories().isEmpty()) {
            log.warn("⚠️ AI не вернул категории для title: '{}'", title);
            return new CategorySuggestionResult(title, Collections.emptyList(), "AI returned no categories");
        }

        // Преобразуем в результат с локализованными названиями
        List<CategorySuggestionResult.CategoryWithConfidence> result = suggestion.getCategories().stream()
                .filter(item -> isValidCategoryKey(item.getKey()))
                .map(item -> {
                    String key = item.getKey().toLowerCase().trim();
                    Optional<Category> cat = categoryRepository.findByKey(key);
                    if (cat.isPresent()) {
                        String localizedName = cat.get().getLocalizedName(language);
                        return new CategorySuggestionResult.CategoryWithConfidence(
                                key,
                                localizedName,
                                item.getConfidence() != null ? item.getConfidence() : 0.0,
                                item.getReason()
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("✅ AI предложил {} категорий для title '{}'", result.size(), title);
        return new CategorySuggestionResult(title, result, null);
    }

    /**
     * МЕТОД #2: Предлагает категории для стикерсета по ID с опцией автоприменения
     * 
     * @param stickerSetId ID стикерсета
     * @param autoApply если true - применяет категории, если false - только предпросмотр
     * @param language язык для локализации
     * @param minConfidence минимальный уровень уверенности для применения категорий (только при autoApply=true)
     * @return результат с предложенными категориями
     */
    @Transactional
    public CategorySuggestionResult suggestCategoriesForStickerSet(Long stickerSetId, boolean autoApply, String language, Double minConfidence) {
        log.info("🤖 Предложение категорий для стикерсета ID: {}, autoApply={}, minConfidence={}", 
            stickerSetId, autoApply, minConfidence);

        // Получаем стикерсет
        StickerSet stickerSet = stickerSetService.findById(stickerSetId);
        if (stickerSet == null) {
            throw new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден");
        }

        String title = stickerSet.getTitle();
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не имеет title для анализа");
        }

        // Вызываем метод #1 для получения предложений
        CategorySuggestionResult result = suggestCategoriesForTitle(title, language);

        // Если autoApply=true, применяем категории с фильтрацией по minConfidence
        if (autoApply && !result.getSuggestedCategories().isEmpty()) {
            List<CategorySuggestionResult.CategoryWithConfidence> categoriesToApply = result.getSuggestedCategories();
            
            // Фильтруем по minConfidence, если он указан
            if (minConfidence != null && minConfidence >= 0.0 && minConfidence <= 1.0) {
                int beforeFilter = categoriesToApply.size();
                categoriesToApply = categoriesToApply.stream()
                        .filter(cat -> cat.getConfidence() != null && cat.getConfidence() >= minConfidence)
                        .collect(Collectors.toList());
                int afterFilter = categoriesToApply.size();
                
                log.info("📊 Фильтрация категорий по minConfidence={}: было {}, осталось {}", 
                    minConfidence, beforeFilter, afterFilter);
                
                if (afterFilter == 0) {
                    log.warn("⚠️ Все категории отфильтрованы по minConfidence={}, ни одна категория не будет применена", 
                        minConfidence);
                }
            }
            
            if (!categoriesToApply.isEmpty()) {
                Set<String> categoryKeys = categoriesToApply.stream()
                        .map(CategorySuggestionResult.CategoryWithConfidence::getCategoryKey)
                        .collect(Collectors.toSet());

                log.info("📝 Применение категорий {} к стикерсету {} (отфильтровано по minConfidence={})", 
                    categoryKeys, stickerSetId, minConfidence);
                stickerSetService.updateCategories(stickerSetId, categoryKeys);
                log.info("✅ Категории успешно применены к стикерсету {}", stickerSetId);
            } else {
                log.info("ℹ️ Нет категорий для применения к стикерсету {} (после фильтрации по minConfidence)", stickerSetId);
            }
        }

        return result;
    }


    // ========== Вспомогательные методы ==========

    /**
     * Форматирует список категорий для промпта
     */
    private String formatCategoriesList(List<Category> categories) {
        if (categories.isEmpty()) {
            return "No categories available";
        }
        return categories.stream()
                .map(cat -> String.format("- %s (%s / %s)", 
                        cat.getKey(), 
                        cat.getNameRu(), 
                        cat.getNameEn()))
                .collect(Collectors.joining("\n"));
    }


    /**
     * Загружает промпт из файла ресурсов
     */
    private String loadPrompt(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("❌ Ошибка при загрузке промпта из {}: {}", path, e.getMessage(), e);
            throw new RuntimeException("Не удалось загрузить промпт из " + path, e);
        }
    }

    /**
     * Парсит JSON ответ от AI для категоризации
     */
    private CategorySuggestionDto parseAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = cleanAIResponse(aiResponse);
            return objectMapper.readValue(cleaned, CategorySuggestionDto.class);
        } catch (Exception e) {
            log.error("❌ Ошибка при парсинге ответа AI: {}", e.getMessage());
            log.debug("Ответ AI для отладки: {}", aiResponse);
            throw new RuntimeException("Не удалось распарсить ответ AI: " + e.getMessage(), e);
        }
    }


    /**
     * Очищает ответ AI от markdown и лишних символов
     */
    private String cleanAIResponse(String response) {
        String cleaned = response.trim();
        
        // Удаляем markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        cleaned = cleaned.trim();
        
        // Извлекаем первый валидный JSON объект
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }
        
        return cleaned.trim();
    }

    /**
     * Вызов Spring AI через AIService (обходит шаблонизатор, т.к. строки уже санитизированы от ${})
     */
    private String callAiWithoutTemplate(String conversationId, String systemText, String userText) {
        try {
            // Используем AIService, который уже работает корректно
            // Строки уже санитизированы от ${} плейсхолдеров, фигурные скобки сохранены для JSON
            return aiService.completion(conversationId, userText, systemText, null);
        } catch (Exception e) {
            log.error("❌ Ошибка вызова AI: {}", e.getMessage(), e);
            throw new RuntimeException("AI call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Валидирует ключ категории
     */
    private boolean isValidCategoryKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        return CATEGORY_KEY_PATTERN.matcher(key.toLowerCase().trim()).matches();
    }

}

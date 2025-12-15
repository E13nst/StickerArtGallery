package com.example.sticker_art_gallery.service.ai;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetDescription;
import com.example.sticker_art_gallery.model.telegram.StickerSetDescriptionRepository;
import com.example.sticker_art_gallery.service.proxy.StickerProxyService;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для генерации многоязычных описаний стикерсетов с помощью AI
 */
@Service
public class StickerSetDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(StickerSetDescriptionService.class);
    private static final String SYSTEM_PROMPT_PATH = "ai/prompts/stickerset_description.system.txt";
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final AIService aiService;
    private final StickerSetService stickerSetService;
    private final StickerSetDescriptionRepository descriptionRepository;
    private final StickerProxyService stickerProxyService;
    private final ObjectMapper objectMapper;

    @Autowired
    public StickerSetDescriptionService(
            AIService aiService,
            StickerSetService stickerSetService,
            StickerSetDescriptionRepository descriptionRepository,
            StickerProxyService stickerProxyService,
            ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.stickerSetService = stickerSetService;
        this.descriptionRepository = descriptionRepository;
        this.stickerProxyService = stickerProxyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Генерирует описания стикерсета на русском и английском языках
     * 
     * @param stickerSetId ID стикерсета
     * @param userId ID пользователя, который запросил генерацию
     * @return Map с описаниями (ключ - язык, значение - описание)
     */
    @Transactional
    public Map<String, String> generateDescriptionForStickerSet(Long stickerSetId, Long userId) {
        log.info("🤖 Генерация описания для стикерсета ID: {}, userId: {}", stickerSetId, userId);

        // 1. Получаем стикерсет
        StickerSet stickerSet = stickerSetService.findById(stickerSetId);
        if (stickerSet == null) {
            throw new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден");
        }

        String name = stickerSet.getName();
        String title = stickerSet.getTitle();

        log.info("📦 Стикерсет: name={}, title={}", name, title);

        // 2. Получаем комбинированное изображение стикеров
        log.info("🖼️ Запрос комбинированного изображения стикерсета...");
        byte[] imageData;
        try {
            imageData = stickerProxyService.getCombinedStickerSetImage(
                name, 
                "thumbnail", // image_type
                128,         // tile_size
                100          // max_stickers
            );
            log.info("✅ Получено изображение размером {} bytes", imageData.length);
        } catch (Exception e) {
            log.error("❌ Ошибка при получении изображения от sticker-processor: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить изображение стикерсета: " + e.getMessage(), e);
        }

        // 3. Загружаем системный промпт
        String systemPrompt = loadPrompt(SYSTEM_PROMPT_PATH);

        // 4. Формируем пользовательский промпт
        String userPrompt = String.format(
            "Analyze the sticker set titled ‘%s’ and provide a description in Russian and English.",
            title
        );

        // 5. Отправляем запрос к ChatGPT с изображением
        log.info("🤖 Отправка запроса к ChatGPT для анализа стикерсета...");
        String conversationId = "description-generation-" + stickerSetId + "-" + System.currentTimeMillis();
        String aiResponse;
        try {
            aiResponse = aiService.completionWithImage(
                conversationId,
                systemPrompt,
                userPrompt,
                imageData,
                "image/webp"
            );
            log.debug("🤖 Ответ ChatGPT: {}", aiResponse);
        } catch (Exception e) {
            log.error("❌ Ошибка при обращении к ChatGPT: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при работе с AI: " + e.getMessage(), e);
        }

        // 6. Парсим JSON ответ
        Map<String, String> descriptions = parseAIResponse(aiResponse);
        if (descriptions == null || descriptions.isEmpty()) {
            throw new RuntimeException("ChatGPT не вернул описания или вернул некорректный формат");
        }

        // 7. Валидируем и обрезаем описания
        String ruDescription = descriptions.get("ru");
        String enDescription = descriptions.get("en");

        if (ruDescription == null || ruDescription.trim().isEmpty()) {
            throw new RuntimeException("ChatGPT не вернул описание на русском языке");
        }
        if (enDescription == null || enDescription.trim().isEmpty()) {
            throw new RuntimeException("ChatGPT не вернул описание на английском языке");
        }

        // Обрезаем до максимальной длины с предупреждением
        if (ruDescription.length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("⚠️ Описание на русском превышает {} символов ({}), обрезаем", 
                    MAX_DESCRIPTION_LENGTH, ruDescription.length());
            ruDescription = ruDescription.substring(0, MAX_DESCRIPTION_LENGTH);
        }
        if (enDescription.length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("⚠️ Описание на английском превышает {} символов ({}), обрезаем", 
                    MAX_DESCRIPTION_LENGTH, enDescription.length());
            enDescription = enDescription.substring(0, MAX_DESCRIPTION_LENGTH);
        }

        // 8. Сохраняем описания в БД (перезаписываем существующие)
        saveDescription(stickerSet, "ru", ruDescription, userId);
        saveDescription(stickerSet, "en", enDescription, userId);

        log.info("✅ Описания успешно сгенерированы и сохранены для стикерсета {}", stickerSetId);

        Map<String, String> result = new HashMap<>();
        result.put("ru", ruDescription);
        result.put("en", enDescription);
        return result;
    }

    /**
     * Сохраняет описание стикерсета на указанном языке
     */
    private void saveDescription(StickerSet stickerSet, String language, String description, Long userId) {
        Optional<StickerSetDescription> existing = descriptionRepository
            .findByStickerSetIdAndLanguage(stickerSet.getId(), language);

        StickerSetDescription desc;
        if (existing.isPresent()) {
            // Обновляем существующее описание
            desc = existing.get();
            desc.setDescription(description);
            desc.setUserId(userId);
            log.debug("📝 Обновлено описание на языке {} для стикерсета {}", language, stickerSet.getId());
        } else {
            // Создаем новое описание
            desc = new StickerSetDescription();
            desc.setStickerSet(stickerSet);
            desc.setLanguage(language);
            desc.setDescription(description);
            desc.setUserId(userId);
            log.debug("✨ Создано новое описание на языке {} для стикерсета {}", language, stickerSet.getId());
        }

        descriptionRepository.save(desc);
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
     * Парсит JSON ответ от AI
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = cleanAIResponse(aiResponse);
            Map<String, Object> response = objectMapper.readValue(cleaned, Map.class);
            
            Map<String, String> descriptions = new HashMap<>();
            if (response.containsKey("ru")) {
                descriptions.put("ru", String.valueOf(response.get("ru")));
            }
            if (response.containsKey("en")) {
                descriptions.put("en", String.valueOf(response.get("en")));
            }
            
            return descriptions;
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
}


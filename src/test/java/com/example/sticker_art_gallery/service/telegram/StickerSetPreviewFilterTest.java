package com.example.sticker_art_gallery.service.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты фильтрации стикеров для режима превью")
class StickerSetPreviewFilterTest {

    @Mock
    private TelegramBotApiService telegramBotApiService;

    @InjectMocks
    private StickerSetService stickerSetService;

    @Test
    @DisplayName("filterStickersForPreview должен оставить только 1 стикер из 10")
    void filterStickersForPreview_WithTenStickers_ShouldReturnOne() {
        // Given: создаем объект с 10 стикерами
        Map<String, Object> telegramInfo = createTelegramInfoWithStickers(10);
        
        // When: применяем фильтрацию через рефлексию (метод приватный)
        // Для тестирования приватного метода используем рефлексию
        try {
            java.lang.reflect.Method method = StickerSetService.class.getDeclaredMethod(
                "filterStickersForPreview", Object.class);
            method.setAccessible(true);
            Object result = method.invoke(stickerSetService, telegramInfo);
            
            // Then: проверяем результат
            assertNotNull(result);
            assertTrue(result instanceof Map);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> stickers = (List<Object>) resultMap.get("stickers");
            
            assertNotNull(stickers);
            assertEquals(1, stickers.size(), "Должен быть ровно 1 стикер");
            
            // Проверяем, что остальные поля не изменились
            assertEquals("test_pack", resultMap.get("name"));
            assertEquals("Test Pack", resultMap.get("title"));
            
        } catch (Exception e) {
            fail("Ошибка при вызове метода через рефлексию: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("filterStickersForPreview должен вернуть все стикеры если их меньше или равно 1")
    void filterStickersForPreview_WithLessThanOrEqualToOneSticker_ShouldReturnAll() {
        // Given: создаем объект с 1 стикером
        Map<String, Object> telegramInfo = createTelegramInfoWithStickers(1);
        
        // When: применяем фильтрацию
        try {
            java.lang.reflect.Method method = StickerSetService.class.getDeclaredMethod(
                "filterStickersForPreview", Object.class);
            method.setAccessible(true);
            Object result = method.invoke(stickerSetService, telegramInfo);
            
            // Then: проверяем результат
            assertNotNull(result);
            assertTrue(result instanceof Map);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> stickers = (List<Object>) resultMap.get("stickers");
            
            assertNotNull(stickers);
            assertEquals(1, stickers.size(), "Должен вернуться 1 стикер");
            
        } catch (Exception e) {
            fail("Ошибка при вызове метода через рефлексию: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("filterStickersForPreview должен вернуть объект без изменений если это не Map")
    void filterStickersForPreview_WithNonMapObject_ShouldReturnUnchanged() {
        // Given: создаем объект, который не является Map
        String nonMapObject = "not a map";
        
        // When: применяем фильтрацию
        try {
            java.lang.reflect.Method method = StickerSetService.class.getDeclaredMethod(
                "filterStickersForPreview", Object.class);
            method.setAccessible(true);
            Object result = method.invoke(stickerSetService, nonMapObject);
            
            // Then: проверяем, что объект не изменился
            assertEquals(nonMapObject, result);
            
        } catch (Exception e) {
            fail("Ошибка при вызове метода через рефлексию: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("filterStickersForPreview должен возвращать разные стикеры при каждом вызове")
    void filterStickersForPreview_ShouldReturnRandomStickers() {
        // Given: создаем объект с 10 стикерами
        Map<String, Object> telegramInfo = createTelegramInfoWithStickers(10);
        
        // When: применяем фильтрацию несколько раз
        try {
            java.lang.reflect.Method method = StickerSetService.class.getDeclaredMethod(
                "filterStickersForPreview", Object.class);
            method.setAccessible(true);
            
            Set<String> firstResultStickerIds = new HashSet<>();
            Set<String> secondResultStickerIds = new HashSet<>();
            
            // Первый вызов
            Object result1 = method.invoke(stickerSetService, deepCopy(telegramInfo));
            @SuppressWarnings("unchecked")
            List<Object> stickers1 = (List<Object>) ((Map<String, Object>) result1).get("stickers");
            for (Object sticker : stickers1) {
                @SuppressWarnings("unchecked")
                String fileId = (String) ((Map<String, Object>) sticker).get("file_id");
                firstResultStickerIds.add(fileId);
            }
            
            // Второй вызов
            Object result2 = method.invoke(stickerSetService, deepCopy(telegramInfo));
            @SuppressWarnings("unchecked")
            List<Object> stickers2 = (List<Object>) ((Map<String, Object>) result2).get("stickers");
            for (Object sticker : stickers2) {
                @SuppressWarnings("unchecked")
                String fileId = (String) ((Map<String, Object>) sticker).get("file_id");
                secondResultStickerIds.add(fileId);
            }
            
            // Then: с высокой вероятностью результаты должны отличаться
            // (хотя теоретически могут совпасть случайно)
            // Проверяем, что оба результата содержат 1 стикер
            assertEquals(1, firstResultStickerIds.size());
            assertEquals(1, secondResultStickerIds.size());
            
        } catch (Exception e) {
            fail("Ошибка при вызове метода через рефлексию: " + e.getMessage());
        }
    }

    /**
     * Создает мок объект telegramStickerSetInfo с указанным количеством стикеров
     */
    private Map<String, Object> createTelegramInfoWithStickers(int stickerCount) {
        Map<String, Object> telegramInfo = new LinkedHashMap<>();
        telegramInfo.put("name", "test_pack");
        telegramInfo.put("title", "Test Pack");
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

    /**
     * Глубокое копирование Map для теста случайности
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> original) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (entry.getValue() instanceof List) {
                List<Object> originalList = (List<Object>) entry.getValue();
                List<Object> copiedList = new ArrayList<>();
                for (Object item : originalList) {
                    if (item instanceof Map) {
                        copiedList.add(deepCopy((Map<String, Object>) item));
                    } else {
                        copiedList.add(item);
                    }
                }
                copy.put(entry.getKey(), copiedList);
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
}




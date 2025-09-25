package com.example.sticker_art_gallery.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Утилиты для работы с AI
 */
public class AIUtils {
    
    /**
     * Разделяет текст на элементы по маркерам
     * 
     * @param text исходный текст
     * @return список элементов
     */
    public static List<String> splitItems(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        
        return Arrays.stream(text.split("\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> line.startsWith("-") || line.startsWith("*") || line.startsWith("•"))
                .map(line -> line.replaceAll("^[-*•]\\s*", "").trim())
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }
} 
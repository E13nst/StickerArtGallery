package com.example.sticker_art_gallery.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Валидатор для проверки корректности имени стикерсета или URL стикерсета
 */
public class StickerSetNameValidator implements ConstraintValidator<ValidStickerSetName, String> {
    
    @Override
    public void initialize(ValidStickerSetName constraintAnnotation) {
        // Инициализация не требуется
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null и пустые значения обрабатываются @NotBlank, но для кастомного валидатора возвращаем true,
        // чтобы не дублировать валидацию
        if (value == null || value.trim().isEmpty()) {
            return true; 
        }
        
        String trimmed = value.trim();
        
        // Проверяем, является ли это URL стикерсета
        if (isStickerSetUrl(trimmed)) {
            return isValidStickerSetUrl(trimmed);
        }
        
        // Проверяем, является ли это именем стикерсета
        return isValidStickerSetName(trimmed);
    }
    
    /**
     * Проверяет, является ли строка URL стикерсета
     */
    private boolean isStickerSetUrl(String input) {
        String lower = input.toLowerCase();
        return lower.startsWith("https://t.me/addstickers/") || 
               lower.startsWith("http://t.me/addstickers/") ||
               lower.startsWith("t.me/addstickers/");
    }
    
    /**
     * Проверяет корректность URL стикерсета
     */
    private boolean isValidStickerSetUrl(String url) {
        try {
            // Упрощенная проверка - просто убеждаемся, что есть имя стикерсета после /addstickers/
            String lower = url.toLowerCase();
            if (lower.contains("t.me/addstickers/")) {
                String afterAddstickers = lower.substring(lower.indexOf("t.me/addstickers/") + "t.me/addstickers/".length());
                if (afterAddstickers.isEmpty() || afterAddstickers.contains("/")) {
                    return false;
                }
                // Убираем параметры URL
                if (afterAddstickers.contains("?")) {
                    afterAddstickers = afterAddstickers.substring(0, afterAddstickers.indexOf("?"));
                }
                // Проверяем, что имя стикерсета содержит только допустимые символы
                return afterAddstickers.matches("^[a-zA-Z0-9_]+$");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Проверяет корректность имени стикерсета
     */
    private boolean isValidStickerSetName(String name) {
        // Имя стикерсета должно содержать только латинские буквы, цифры и подчеркивания
        return name.matches("^[a-zA-Z0-9_]+$");
    }
}

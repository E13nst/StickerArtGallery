package com.example.sticker_art_gallery.service.ai;

public interface AIService {
    String completion(String conversationId, String message, String prompt, Integer memWindow);
    
    /**
     * Выполняет запрос к AI с изображением
     * @param conversationId идентификатор разговора
     * @param systemPrompt системный промпт
     * @param userPrompt пользовательский промпт
     * @param imageData данные изображения в виде байтов
     * @param imageMimeType MIME тип изображения (например, "image/webp")
     * @return ответ от AI
     */
    String completionWithImage(String conversationId, String systemPrompt, String userPrompt, 
                                byte[] imageData, String imageMimeType);
}

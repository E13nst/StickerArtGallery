package com.example.sticker_art_gallery.exception;

/**
 * Исключение для ошибок AI сервиса
 */
public class AIServiceException extends BotException {
    
    public AIServiceException(String message) {
        super(message);
    }
    
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
} 
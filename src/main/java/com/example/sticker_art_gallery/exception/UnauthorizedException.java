package com.example.sticker_art_gallery.exception;

/**
 * Исключение, выбрасываемое при попытке доступа к ресурсу без необходимой авторизации
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}


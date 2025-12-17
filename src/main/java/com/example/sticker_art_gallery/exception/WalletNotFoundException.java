package com.example.sticker_art_gallery.exception;

/**
 * Исключение, выбрасываемое когда активный кошелёк не найден
 */
public class WalletNotFoundException extends RuntimeException {
    
    public WalletNotFoundException(Long userId) {
        super("Active wallet not found for user: " + userId);
    }
    
    public WalletNotFoundException(String message) {
        super(message);
    }
}






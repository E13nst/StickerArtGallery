package com.example.sticker_art_gallery.exception;

/**
 * Исключение, выбрасываемое когда стикерсет не найден
 */
public class StickerSetNotFoundException extends RuntimeException {
    
    public StickerSetNotFoundException(Long stickerSetId) {
        super("StickerSet not found: " + stickerSetId);
    }
}








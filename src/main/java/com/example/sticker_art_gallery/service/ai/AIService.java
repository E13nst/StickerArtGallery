package com.example.sticker_art_gallery.service.ai;

public interface AIService {
    String completion(String conversationId, String message, String prompt, Integer memWindow);
}

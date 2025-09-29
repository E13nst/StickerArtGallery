package com.example.sticker_art_gallery.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Контроллер для React мини-приложения Telegram Web App
 */
@Controller
@RequestMapping("/mini-app-react")
public class MiniAppReactController {
    
    /**
     * Главная страница React мини-приложения
     */
    @GetMapping({"", "/", "/index.html"})
    public ResponseEntity<String> index() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/mini-app-react/index.html");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }
}

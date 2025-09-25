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
 * Контроллер для мини-приложения Telegram Web App
 */
@Controller
@RequestMapping("/mini-app")
public class MiniAppController {
    
    /**
     * Главная страница мини-приложения
     */
    @GetMapping("/index.html")
    public ResponseEntity<String> index() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/mini-app/index.html");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }
    
    /**
     * JavaScript файл мини-приложения
     */
    @GetMapping("/app.js")
    public ResponseEntity<String> appJs() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/mini-app/app.js");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(content);
    }
    
    /**
     * CSS файл мини-приложения
     */
    @GetMapping("/style.css")
    public ResponseEntity<String> styleCss() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/mini-app/style.css");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/css"))
                .body(content);
    }
}

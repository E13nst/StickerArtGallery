package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class TelegramWebhookController {
    private final BotService botService;
    private final Map<String, AbstractTelegramBot> botRegistry;

    @Autowired
    public TelegramWebhookController(BotService botService, Map<String, AbstractTelegramBot> botRegistry) {
        this.botService = botService;
        this.botRegistry = botRegistry;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<String> handleWebhook(@PathVariable String botUsername, @RequestBody Update update) {
        AbstractTelegramBot bot = botRegistry.get(botUsername);
        if (bot != null) {
            bot.onUpdateReceived(update);
            log.info("✅ Update routed to bot: {}", botUsername);
            return ResponseEntity.ok("OK");
        } else {
            log.warn("❌ No bot found for username: {}", botUsername);
            return ResponseEntity.notFound().build();
        }
    }
} 
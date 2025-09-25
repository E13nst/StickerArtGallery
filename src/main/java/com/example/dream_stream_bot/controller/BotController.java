package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.dto.BotEntityDto;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bots")
@CrossOrigin(origins = "*")
public class BotController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BotController.class);
    private final BotService botService;
    
    @Autowired
    public BotController(BotService botService) {
        this.botService = botService;
    }
    
    /**
     * Получить всех ботов
     */
    @GetMapping
    public ResponseEntity<List<BotEntityDto>> getAllBots() {
        try {
            LOGGER.info("📋 Получение всех ботов");
            List<BotEntity> bots = botService.findAll();
            List<BotEntityDto> dtos = bots.stream()
                    .map(BotEntityDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} ботов", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении всех ботов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить бота по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BotEntityDto> getBotById(@PathVariable Long id) {
        try {
            LOGGER.info("🔍 Поиск бота по ID: {}", id);
            BotEntity bot = botService.findById(id);
            
            if (bot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            BotEntityDto dto = BotEntityDto.fromEntity(bot);
            LOGGER.info("✅ Бот найден: {}", dto.getName());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить ботов по типу
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<BotEntityDto>> getBotsByType(@PathVariable String type) {
        try {
            LOGGER.info("🔍 Поиск ботов по типу: {}", type);
            List<BotEntity> bots = botService.findByType(type);
            List<BotEntityDto> dtos = bots.stream()
                    .map(BotEntityDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} ботов типа {}", dtos.size(), type);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске ботов типа: {}", type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить активных ботов
     */
    @GetMapping("/active")
    public ResponseEntity<List<BotEntityDto>> getActiveBots() {
        try {
            LOGGER.info("🔍 Получение активных ботов");
            List<BotEntity> bots = botService.findActiveBots();
            List<BotEntityDto> dtos = bots.stream()
                    .map(BotEntityDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} активных ботов", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении активных ботов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить бота по имени
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<BotEntityDto> getBotByName(@PathVariable String name) {
        try {
            LOGGER.info("🔍 Поиск бота по имени: {}", name);
            
            List<BotEntity> bots = botService.findAll();
            BotEntity targetBot = bots.stream()
                    .filter(bot -> name.equals(bot.getName()))
                    .findFirst()
                    .orElse(null);
            
            if (targetBot == null) {
                LOGGER.warn("⚠️ Бот с именем '{}' не найден", name);
                return ResponseEntity.notFound().build();
            }
            
            BotEntityDto dto = BotEntityDto.fromEntity(targetBot);
            LOGGER.info("✅ Бот найден: {}", dto.getName());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске бота с именем: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить miniapp для бота по имени
     */
    @PatchMapping("/{name}/miniapp")
    public ResponseEntity<BotEntityDto> updateMiniapp(@PathVariable String name, @RequestBody String miniappUrl) {
        try {
            LOGGER.info("🔧 Обновление miniapp для бота '{}': {}", name, miniappUrl);
            
            // Находим бота по имени
            List<BotEntity> bots = botService.findAll();
            BotEntity targetBot = bots.stream()
                    .filter(bot -> name.equals(bot.getName()))
                    .findFirst()
                    .orElse(null);
            
            if (targetBot == null) {
                LOGGER.warn("⚠️ Бот с именем '{}' не найден", name);
                return ResponseEntity.notFound().build();
            }
            
            // Обновляем miniapp
            targetBot.setMiniapp(miniappUrl);
            BotEntity updatedBot = botService.save(targetBot);
            
            BotEntityDto dto = BotEntityDto.fromEntity(updatedBot);
            LOGGER.info("✅ Miniapp обновлен для бота '{}': {}", name, miniappUrl);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении miniapp для бота '{}'", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Создать нового бота
     */
    @PostMapping
    public ResponseEntity<BotEntityDto> createBot(@RequestBody BotEntityDto botDto) {
        try {
            LOGGER.info("➕ Создание нового бота: {}", botDto.getName());
            
            if (botDto.getName() == null || botDto.getUsername() == null || botDto.getToken() == null || botDto.getType() == null) {
                LOGGER.warn("⚠️ Неполные данные для создания бота");
                return ResponseEntity.badRequest().build();
            }
            
            BotEntity newBot = new BotEntity();
            newBot.setName(botDto.getName());
            newBot.setUsername(botDto.getUsername());
            newBot.setToken(botDto.getToken());
            newBot.setType(botDto.getType());
            newBot.setPrompt(botDto.getPrompt());
            newBot.setWebhookUrl(botDto.getWebhookUrl());
            newBot.setDescription(botDto.getDescription());
            newBot.setTriggers(botDto.getTriggers());
            newBot.setMemWindow(botDto.getMemWindow());
            newBot.setMiniapp(botDto.getMiniapp());
            newBot.setIsActive(botDto.getIsActive() != null ? botDto.getIsActive() : true);
            
            BotEntity savedBot = botService.save(newBot);
            BotEntityDto createdDto = BotEntityDto.fromEntity(savedBot);
            
            LOGGER.info("✅ Бот создан с ID: {}", createdDto.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании бота", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить существующего бота
     */
    @PutMapping("/{id}")
    public ResponseEntity<BotEntityDto> updateBot(@PathVariable Long id, @RequestBody BotEntityDto botDto) {
        try {
            LOGGER.info("✏️ Обновление бота с ID: {}", id);
            
            BotEntity existingBot = botService.findById(id);
            if (existingBot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден для обновления", id);
                return ResponseEntity.notFound().build();
            }
            
            // Обновляем поля
            if (botDto.getName() != null) {
                existingBot.setName(botDto.getName());
            }
            if (botDto.getUsername() != null) {
                existingBot.setUsername(botDto.getUsername());
            }
            if (botDto.getToken() != null) {
                existingBot.setToken(botDto.getToken());
            }
            if (botDto.getType() != null) {
                existingBot.setType(botDto.getType());
            }
            if (botDto.getPrompt() != null) {
                existingBot.setPrompt(botDto.getPrompt());
            }
            if (botDto.getWebhookUrl() != null) {
                existingBot.setWebhookUrl(botDto.getWebhookUrl());
            }
            if (botDto.getDescription() != null) {
                existingBot.setDescription(botDto.getDescription());
            }
            if (botDto.getTriggers() != null) {
                existingBot.setTriggers(botDto.getTriggers());
            }
            if (botDto.getMemWindow() != null) {
                existingBot.setMemWindow(botDto.getMemWindow());
            }
            if (botDto.getMiniapp() != null) {
                existingBot.setMiniapp(botDto.getMiniapp());
            }
            if (botDto.getIsActive() != null) {
                existingBot.setIsActive(botDto.getIsActive());
            }
            
            BotEntity updatedBot = botService.save(existingBot);
            BotEntityDto updatedDto = BotEntityDto.fromEntity(updatedBot);
            
            LOGGER.info("✅ Бот обновлен: {}", updatedDto.getName());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить только miniapp для бота по ID
     */
    @PatchMapping("/{id}/miniapp")
    public ResponseEntity<BotEntityDto> updateBotMiniapp(@PathVariable Long id, @RequestBody String miniappUrl) {
        try {
            LOGGER.info("🔧 Обновление miniapp для бота с ID {}: {}", id, miniappUrl);
            
            BotEntity existingBot = botService.findById(id);
            if (existingBot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            existingBot.setMiniapp(miniappUrl);
            BotEntity updatedBot = botService.save(existingBot);
            BotEntityDto updatedDto = BotEntityDto.fromEntity(updatedBot);
            
            LOGGER.info("✅ Miniapp обновлен для бота с ID {}: {}", id, miniappUrl);
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении miniapp для бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Удалить бота
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBot(@PathVariable Long id) {
        try {
            LOGGER.info("🗑️ Удаление бота с ID: {}", id);
            
            BotEntity existingBot = botService.findById(id);
            if (existingBot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден для удаления", id);
                return ResponseEntity.notFound().build();
            }
            
            botService.deleteById(id);
            LOGGER.info("✅ Бот с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

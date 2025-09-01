package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.dto.StickerSetDto;
import com.example.dream_stream_bot.model.telegram.StickerSet;
import com.example.dream_stream_bot.service.telegram.StickerSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*") // Разрешаем CORS для фронтенда
public class StickerSetController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetController.class);
    private final StickerSetService stickerSetService;
    
    @Autowired
    public StickerSetController(StickerSetService stickerSetService) {
        this.stickerSetService = stickerSetService;
    }
    
    /**
     * Получить все стикерсеты
     */
    @GetMapping
    public ResponseEntity<List<StickerSetDto>> getAllStickerSets() {
        try {
            LOGGER.info("📋 Получение всех стикерсетов");
            List<StickerSet> stickerSets = stickerSetService.findAll();
            List<StickerSetDto> dtos = stickerSets.stream()
                    .map(StickerSetDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} стикерсетов", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении всех стикерсетов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить стикерсет по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StickerSetDto> getStickerSetById(@PathVariable Long id) {
        try {
            LOGGER.info("🔍 Поиск стикерсета по ID: {}", id);
            StickerSet stickerSet = stickerSetService.findById(id);
            
            if (stickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            StickerSetDto dto = StickerSetDto.fromEntity(stickerSet);
            LOGGER.info("✅ Стикерсет найден: {}", dto.getTitle());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить стикерсеты по ID пользователя
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StickerSetDto>> getStickerSetsByUserId(@PathVariable Long userId) {
        try {
            LOGGER.info("🔍 Поиск стикерсетов для пользователя: {}", userId);
            List<StickerSet> stickerSets = stickerSetService.findByUserId(userId);
            List<StickerSetDto> dtos = stickerSets.stream()
                    .map(StickerSetDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} стикерсетов для пользователя {}", dtos.size(), userId);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсетов для пользователя: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить стикерсет по названию
     */
    @GetMapping("/search")
    public ResponseEntity<StickerSetDto> getStickerSetByName(@RequestParam String name) {
        try {
            LOGGER.info("🔍 Поиск стикерсета по названию: {}", name);
            StickerSet stickerSet = stickerSetService.findByName(name);
            
            if (stickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с названием '{}' не найден", name);
                return ResponseEntity.notFound().build();
            }
            
            StickerSetDto dto = StickerSetDto.fromEntity(stickerSet);
            LOGGER.info("✅ Стикерсет найден: {}", dto.getTitle());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсета с названием: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Создать новый стикерсет
     */
    @PostMapping
    public ResponseEntity<StickerSetDto> createStickerSet(@RequestBody StickerSetDto stickerSetDto) {
        try {
            LOGGER.info("➕ Создание нового стикерсета: {}", stickerSetDto.getTitle());
            
            if (stickerSetDto.getUserId() == null || stickerSetDto.getTitle() == null || stickerSetDto.getName() == null) {
                LOGGER.warn("⚠️ Неполные данные для создания стикерсета");
                return ResponseEntity.badRequest().build();
            }
            
            StickerSet newStickerSet = stickerSetService.createStickerSet(
                stickerSetDto.getUserId(),
                stickerSetDto.getTitle(),
                stickerSetDto.getName()
            );
            
            StickerSetDto createdDto = StickerSetDto.fromEntity(newStickerSet);
            LOGGER.info("✅ Стикерсет создан с ID: {}", createdDto.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании стикерсета", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить существующий стикерсет
     */
    @PutMapping("/{id}")
    public ResponseEntity<StickerSetDto> updateStickerSet(@PathVariable Long id, @RequestBody StickerSetDto stickerSetDto) {
        try {
            LOGGER.info("✏️ Обновление стикерсета с ID: {}", id);
            
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден для обновления", id);
                return ResponseEntity.notFound().build();
            }
            
            // Обновляем поля
            if (stickerSetDto.getTitle() != null) {
                existingStickerSet.setTitle(stickerSetDto.getTitle());
            }
            if (stickerSetDto.getName() != null) {
                existingStickerSet.setName(stickerSetDto.getName());
            }
            
            StickerSet updatedStickerSet = stickerSetService.save(existingStickerSet);
            StickerSetDto updatedDto = StickerSetDto.fromEntity(updatedStickerSet);
            
            LOGGER.info("✅ Стикерсет обновлен: {}", updatedDto.getTitle());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Удалить стикерсет
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStickerSet(@PathVariable Long id) {
        try {
            LOGGER.info("🗑️ Удаление стикерсета с ID: {}", id);
            
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден для удаления", id);
                return ResponseEntity.notFound().build();
            }
            
            stickerSetService.deleteById(id);
            LOGGER.info("✅ Стикерсет с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 
package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BotService {
    private final BotRepository botRepository;

    @Autowired
    public BotService(BotRepository botRepository) {
        this.botRepository = botRepository;
    }

    public List<BotEntity> getAllBots() {
        return botRepository.findAll();
    }
    
    public List<BotEntity> findAll() {
        return botRepository.findAll();
    }
    
    public BotEntity findById(Long id) {
        return botRepository.findById(id).orElse(null);
    }
    
    public List<BotEntity> findByType(String type) {
        return botRepository.findAll().stream()
                .filter(bot -> bot.getType().equalsIgnoreCase(type))
                .toList();
    }
    
    public List<BotEntity> findActiveBots() {
        return botRepository.findAll().stream()
                .filter(bot -> Boolean.TRUE.equals(bot.getIsActive()))
                .toList();
    }
    
    public BotEntity save(BotEntity bot) {
        return botRepository.save(bot);
    }
    
    public void deleteById(Long id) {
        botRepository.deleteById(id);
    }
    
    /**
     * Поиск бота по имени
     */
    public Optional<BotEntity> findByName(String name) {
        return botRepository.findAll().stream()
                .filter(bot -> name.equals(bot.getName()))
                .findFirst();
    }
} 
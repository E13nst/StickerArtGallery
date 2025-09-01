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

    public Optional<BotEntity> getBotById(Long id) {
        return botRepository.findById(id);
    }

    // Пример: поиск по username
    public Optional<BotEntity> getBotByUsername(String username) {
        return botRepository.findAll().stream()
                .filter(bot -> bot.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    // Пример: поиск по типу
    public List<BotEntity> getBotsByType(String type) {
        return botRepository.findAll().stream()
                .filter(bot -> bot.getType().equalsIgnoreCase(type))
                .toList();
    }

    public BotEntity saveBot(BotEntity bot) {
        return botRepository.save(bot);
    }

    public void deleteBot(Long id) {
        botRepository.deleteById(id);
    }
} 
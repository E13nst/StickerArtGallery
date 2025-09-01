package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.StickerSet;
import com.example.dream_stream_bot.model.telegram.StickerSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StickerSetService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetService.class);
    private final StickerSetRepository stickerSetRepository;
    
    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository) {
        this.stickerSetRepository = stickerSetRepository;
    }
    
    public StickerSet createStickerSet(Long userId, String title, String name) {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(title);
        stickerSet.setName(name);

        StickerSet savedSet = stickerSetRepository.save(stickerSet);

        return savedSet;
    }

    public StickerSet findByName(String name) {
        return stickerSetRepository.findByName(name).orElse(null);
    }

    public StickerSet findByTitle(String title) {
        return stickerSetRepository.findByTitle(title);
    }

    public List<StickerSet> findByUserId(Long userId) {
        return stickerSetRepository.findByUserId(userId);
    }

    public StickerSet findById(Long id) {
        return stickerSetRepository.findById(id).orElse(null);
    }
    
    public List<StickerSet> findAll() {
        return stickerSetRepository.findAll();
    }
    
    public StickerSet save(StickerSet stickerSet) {
        return stickerSetRepository.save(stickerSet);
    }
    
    public void deleteById(Long id) {
        stickerSetRepository.deleteById(id);
    }
} 
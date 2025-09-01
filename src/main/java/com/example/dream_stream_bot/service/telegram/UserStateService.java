package com.example.dream_stream_bot.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserStateService.class);
    
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, StickerSetData> userData = new ConcurrentHashMap<>();
    private final Map<Long, Long> selectedSetIds = new ConcurrentHashMap<>();
    
    public enum UserState {
        WAITING_FOR_PACK_TITLE,
        WAITING_FOR_PACK_NAME
    }
    
    public static class StickerSetData {
        private String title;
        private String name;
        
        public StickerSetData() {}
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public UserState getUserState(Long userId) {
        return userStates.getOrDefault(userId, null);
    }
    
    public void setUserState(Long userId, UserState state) {
        LOGGER.info("🔧 Устанавливаем состояние для пользователя {}: {}", userId, state);
        userStates.put(userId, state);
    }
    
    public void clearUserState(Long userId) {
        LOGGER.info("🧹 Очищаем состояние пользователя: {}", userId);
        userStates.remove(userId);
        userData.remove(userId);
    }
    
    public void setStickerSetData(Long userId, StickerSetData data) {
        LOGGER.info("💾 Сохраняем данные стикерсета для пользователя {}: title='{}', name='{}'", 
                userId, data.getTitle(), data.getName());
        userData.put(userId, data);
    }
    
    public StickerSetData getStickerSetData(Long userId) {
        StickerSetData data = userData.get(userId);
        LOGGER.info("📖 Получаем данные стикерсета для пользователя {}: {}", userId, data != null ? "найдены" : "не найдены");
        return data;
    }
    
    public void setSelectedSetId(Long userId, Long setId) {
        LOGGER.info("🎯 Устанавливаем выбранный стикерсет для пользователя {}: ID={}", userId, setId);
        selectedSetIds.put(userId, setId);
    }
    
    public Long getSelectedSetId(Long userId) {
        Long setId = selectedSetIds.get(userId);
        LOGGER.info("🎯 Получаем выбранный стикерсет для пользователя {}: ID={}", userId, setId);
        return setId;
    }
    
    public void clearSelectedSetId(Long userId) {
        LOGGER.info("🧹 Очищаем выбранный стикерсет для пользователя: {}", userId);
        selectedSetIds.remove(userId);
    }
} 
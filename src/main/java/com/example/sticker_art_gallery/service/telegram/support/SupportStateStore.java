package com.example.sticker_art_gallery.service.telegram.support;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory хранилище state для support bridge.
 * Для wave1 достаточно оперативного хранилища (без БД миграций).
 */
@Service
public class SupportStateStore {

    private final ConcurrentHashMap<Long, String> userSelectedTopic = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> userTopicThread = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> supportMsgToUser = new ConcurrentHashMap<>();

    public void setUserTopic(long userId, String topic) {
        userSelectedTopic.put(userId, topic);
    }

    public String getUserTopic(long userId) {
        return userSelectedTopic.get(userId);
    }

    public void clearUserTopic(long userId) {
        userSelectedTopic.remove(userId);
    }

    public Integer getThreadForUserTopic(long userId, String topic) {
        return userTopicThread.get(key(userId, topic));
    }

    public void saveThreadForUserTopic(long userId, String topic, int threadId) {
        userTopicThread.put(key(userId, topic), threadId);
    }

    public void saveSupportMessageMapping(long supportMessageId, long userId) {
        supportMsgToUser.put(supportMessageId, userId);
    }

    public Long findUserBySupportMessageId(long supportMessageId) {
        return supportMsgToUser.get(supportMessageId);
    }

    private String key(long userId, String topic) {
        return userId + ":" + (topic == null ? "other" : topic);
    }
}

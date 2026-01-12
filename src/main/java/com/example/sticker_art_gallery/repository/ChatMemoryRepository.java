package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.service.memory.ChatMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("postgresChatMemoryRepository")
public interface ChatMemoryRepository extends JpaRepository<ChatMemoryEntity, Long> {
    List<ChatMemoryEntity> findByConversationIdOrderByMessageIndexAsc(String conversationId);
    int countByConversationId(String conversationId);
    void deleteByConversationId(String conversationId);
}

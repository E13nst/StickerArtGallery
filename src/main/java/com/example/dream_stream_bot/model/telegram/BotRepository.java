package com.example.dream_stream_bot.model.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotRepository extends JpaRepository<BotEntity, Long> {
    // Можно добавить методы поиска по username, type и т.д. при необходимости
} 
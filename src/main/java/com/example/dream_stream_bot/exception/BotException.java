package com.example.dream_stream_bot.exception;

/**
 * Базовое исключение для всех ошибок бота
 */
public class BotException extends RuntimeException {
    
    public BotException(String message) {
        super(message);
    }
    
    public BotException(String message, Throwable cause) {
        super(message, cause);
    }
} 
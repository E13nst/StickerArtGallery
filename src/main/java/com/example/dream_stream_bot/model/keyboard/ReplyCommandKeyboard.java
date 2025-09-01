package com.example.dream_stream_bot.model.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class ReplyCommandKeyboard {

    private final ReplyKeyboardMarkup keyboard;
    private final List<KeyboardRow> rows;

    public ReplyCommandKeyboard() {
        this.keyboard = new ReplyKeyboardMarkup();
        this.rows = new ArrayList<>();
    }

    public ReplyCommandKeyboard addKey(String key) {
        if (rows.isEmpty() || rows.get(rows.size() - 1).size() >= 2) {
            // Создаем новую строку если текущая пустая или содержит 2 кнопки
            rows.add(new KeyboardRow());
        }
        rows.get(rows.size() - 1).add(new KeyboardButton(key));
        return this;
    }

    public ReplyKeyboardMarkup build() {
        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setSelective(false);
        return keyboard;
    }
}

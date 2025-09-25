package com.example.sticker_art_gallery.model.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardMarkupBuilder {

    private final InlineKeyboardMarkup keyboard;
    private final List<List<InlineKeyboardButton>> rows;

    public InlineKeyboardMarkupBuilder() {
        this.keyboard = new InlineKeyboardMarkup();
        this.rows = new ArrayList<>();
    }

    /**
     * Добавляет кнопку в текущую строку (или создает новую, если строк нет)
     */
    public InlineKeyboardMarkupBuilder addKey(String text, String callbackData) {
        if (rows.isEmpty()) {
            rows.add(new ArrayList<>());
        }
        
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        
        rows.get(rows.size() - 1).add(button);
        return this;
    }

    /**
     * Добавляет кнопку на отдельную строку
     */
    public InlineKeyboardMarkupBuilder addButtonOnNewRow(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        row.add(button);
        rows.add(row);
        System.out.println("🔧 InlineKeyboardMarkupBuilder: Создана новая строка для кнопки '" + text + "'");
        return this;
    }

    /**
     * Добавляет несколько кнопок в одну строку
     */
    public InlineKeyboardMarkupBuilder addRow(String... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (String buttonText : buttons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData(buttonText.toLowerCase().replace(" ", "_"));
            row.add(button);
        }
        rows.add(row);
        return this;
    }

    /**
     * Добавляет кнопку с указанным callback'ом на отдельную строку
     */
    public InlineKeyboardMarkupBuilder addRow(String buttonText, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);
        row.add(button);
        rows.add(row);
        return this;
    }

    /**
     * Добавляет кнопки навигации (номера страниц) в одну строку
     */
    public InlineKeyboardMarkupBuilder addPageNavigation(int currentPage, int totalPages) {
        if (totalPages > 1) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(String.valueOf(i + 1));
                button.setCallbackData(String.valueOf(i + 1));
                row.add(button);
            }
            rows.add(row);
        }
        return this;
    }

    /**
     * Добавляет информационный текст (не кликабельный) на отдельную строку
     */
    public InlineKeyboardMarkupBuilder addInfoRow(String text) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData("info_" + System.currentTimeMillis()); // Уникальный callback для информации
        rows.add(row);
        return this;
    }

    /**
     * Создает клавиатуру
     */
    public InlineKeyboardMarkup build() {
        keyboard.setKeyboard(rows);
        return keyboard;
    }
}

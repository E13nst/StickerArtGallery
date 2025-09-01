package com.example.dream_stream_bot.model.telegram;

public enum Commands {

    START("start"),
    PREVIOUS("previous"),
    NEXT ("next");

    private final String title;

    Commands(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
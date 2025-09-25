package com.example.sticker_art_gallery.model.keyboard;

import java.util.Optional;

public enum ReplyButtons {

    PREVIOUS("Previous"),
    NEXT("Next"),
    CANCEL("Cancel"),
    NONE("-");

    private final String title;

    ReplyButtons(String title) {
        this.title = title;

    }

    @Override
    public String toString() {
        return title;
    }

    public static ReplyButtons fromTitle(String title) {
        for (ReplyButtons button : values()) {
            if (button.title.equalsIgnoreCase(title)) {
                return button;
            }
        }
        return ReplyButtons.NONE;
    }
}

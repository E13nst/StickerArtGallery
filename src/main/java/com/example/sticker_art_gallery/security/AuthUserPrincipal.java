package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;

public class AuthUserPrincipal {
    private final Long userId;
    private final UserProfileEntity.UserRole role;

    public AuthUserPrincipal(Long userId, UserProfileEntity.UserRole role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public UserProfileEntity.UserRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return String.valueOf(userId);
    }
}



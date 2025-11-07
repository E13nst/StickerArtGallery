package com.example.sticker_art_gallery.service.profile;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public Optional<UserProfileEntity> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<UserProfileEntity> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public Optional<UserProfileEntity> findByTelegramId(Long telegramId) {
        return repository.findByUserId(telegramId);
    }

    public UserProfileEntity getOrCreateDefault(Long userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            UserProfileEntity profile = new UserProfileEntity();
            profile.setUserId(userId);
            profile.setRole(UserProfileEntity.UserRole.USER);
            profile.setArtBalance(0L);
            profile.setIsBlocked(false);
            return repository.save(profile);
        });
    }

    public UserProfileEntity save(UserProfileEntity profile) {
        return repository.save(profile);
    }

    public UserProfileEntity updateArtBalance(Long userId, Long newBalance) {
        UserProfileEntity profile = getOrCreateDefault(userId);
        profile.setArtBalance(newBalance);
        return repository.save(profile);
    }

    public UserProfileEntity addToArtBalance(Long userId, Long amount) {
        UserProfileEntity profile = getOrCreateDefault(userId);
        profile.setArtBalance(profile.getArtBalance() + amount);
        return repository.save(profile);
    }
}



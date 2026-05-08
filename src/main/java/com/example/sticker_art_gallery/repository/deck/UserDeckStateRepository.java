package com.example.sticker_art_gallery.repository.deck;

import com.example.sticker_art_gallery.model.deck.UserDeckStateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserDeckStateRepository extends JpaRepository<UserDeckStateEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserDeckStateEntity s WHERE s.userId = :userId")
    Optional<UserDeckStateEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}

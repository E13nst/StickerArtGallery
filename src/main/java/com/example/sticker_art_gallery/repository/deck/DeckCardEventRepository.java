package com.example.sticker_art_gallery.repository.deck;

import com.example.sticker_art_gallery.model.deck.DeckCardEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCardEventRepository extends JpaRepository<DeckCardEventEntity, Long> {

    boolean existsByUserIdAndCardKeyAndAction(Long userId, String cardKey, String action);
}

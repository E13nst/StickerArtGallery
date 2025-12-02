package com.example.sticker_art_gallery.model.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerSetDescriptionRepository extends JpaRepository<StickerSetDescription, Long> {
    
    /**
     * Найти все описания для стикерсета
     */
    List<StickerSetDescription> findByStickerSetId(Long stickersetId);
    
    /**
     * Найти описание для стикерсета на конкретном языке
     */
    Optional<StickerSetDescription> findByStickerSetIdAndLanguage(Long stickersetId, String language);
    
    /**
     * Удалить все описания для стикерсета
     */
    void deleteByStickerSetId(Long stickersetId);
    
    /**
     * Проверить существование описания для стикерсета на конкретном языке
     */
    boolean existsByStickerSetIdAndLanguage(Long stickersetId, String language);
}


package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StickerSetDto URL generation")
class StickerSetDtoTest {

    @Test
    @DisplayName("Конструктор формирует url на основе name")
    void constructorShouldBuildUrlFromName() {
        StickerSetDto dto = new StickerSetDto(1L, 2L, "Title", "my_pack", LocalDateTime.now());

        assertEquals("https://t.me/addstickers/my_pack", dto.getUrl());
    }

    @Test
    @DisplayName("Изменение name пересчитывает url")
    void setNameShouldUpdateUrl() {
        StickerSetDto dto = new StickerSetDto();

        dto.setName("first_pack");
        assertEquals("https://t.me/addstickers/first_pack", dto.getUrl());

        dto.setName("second_pack");
        assertEquals("https://t.me/addstickers/second_pack", dto.getUrl());
    }

    @Test
    @DisplayName("Пустое или null имя сбрасывает url")
    void blankNameShouldClearUrl() {
        StickerSetDto dto = new StickerSetDto();

        dto.setName(null);
        assertNull(dto.getUrl());

        dto.setName("   ");
        assertNull(dto.getUrl());
    }

    @Test
    @DisplayName("fromEntity заполняет url из name стикерсета")
    void fromEntityShouldPopulateUrl() {
        StickerSet entity = new StickerSet();
        entity.setId(10L);
        entity.setUserId(20L);
        entity.setTitle("Some title");
        entity.setName("entity_pack");
        entity.setIsPublic(true);
        entity.setIsBlocked(false);
        entity.setIsOfficial(false);
        entity.setLikesCount(0);

        StickerSetDto dto = StickerSetDto.fromEntity(entity);

        assertEquals("https://t.me/addstickers/entity_pack", dto.getUrl());
    }

    @Test
    @DisplayName("fromEntity устанавливает пустой список действий, если не передан currentUserId")
    void fromEntity_WithoutCurrentUserId_ShouldSetEmptyActions() {
        // Given
        StickerSet entity = new StickerSet();
        entity.setId(10L);
        entity.setUserId(20L);
        entity.setTitle("Some title");
        entity.setName("entity_pack");
        entity.setIsPublic(true);
        entity.setIsBlocked(false);
        entity.setIsOfficial(false);
        entity.setLikesCount(0);

        // When
        StickerSetDto dto = StickerSetDto.fromEntity(entity);

        // Then
        assertNotNull(dto.getAvailableActions());
        assertTrue(dto.getAvailableActions().isEmpty());
    }
}


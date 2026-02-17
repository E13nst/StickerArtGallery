package com.example.sticker_art_gallery.testdata;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;

import java.time.LocalDateTime;

/**
 * Builder для создания тестовых объектов StickerSet с fluent API
 * Упрощает создание тестовых стикерсетов и уменьшает дублирование кода
 */
public class StickerSetTestBuilder {
    
    private Long userId;
    private String name;
    private String title;
    private String description;
    private StickerSetState state = StickerSetState.ACTIVE;
    private StickerSetVisibility visibility = StickerSetVisibility.PUBLIC;
    private StickerSetType type = StickerSetType.USER;
    private Boolean isVerified;
    private String blockReason;
    private LocalDateTime deletedAt;
    private Integer likesCount = 0;
    
    private StickerSetTestBuilder() {
        // Private constructor - use builder() method
    }
    
    /**
     * Создает новый builder
     */
    public static StickerSetTestBuilder builder() {
        return new StickerSetTestBuilder();
    }
    
    /**
     * Создает builder с базовыми значениями для тестов
     */
    public static StickerSetTestBuilder defaultTest() {
        return builder()
                .withState(StickerSetState.ACTIVE)
                .withVisibility(StickerSetVisibility.PUBLIC)
                .withType(StickerSetType.USER)
                .withLikesCount(0);
    }
    
    public StickerSetTestBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }
    
    public StickerSetTestBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public StickerSetTestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }
    
    public StickerSetTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }
    
    public StickerSetTestBuilder withState(StickerSetState state) {
        this.state = state;
        return this;
    }
    
    public StickerSetTestBuilder withVisibility(StickerSetVisibility visibility) {
        this.visibility = visibility;
        return this;
    }
    
    public StickerSetTestBuilder withType(StickerSetType type) {
        this.type = type;
        return this;
    }
    
    public StickerSetTestBuilder withIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
        return this;
    }
    
    public StickerSetTestBuilder withBlockReason(String blockReason) {
        this.blockReason = blockReason;
        return this;
    }
    
    public StickerSetTestBuilder withDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
        return this;
    }
    
    public StickerSetTestBuilder withLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
        return this;
    }
    
    /**
     * Устанавливает стикерсет как официальный
     */
    public StickerSetTestBuilder asOfficial() {
        this.type = StickerSetType.OFFICIAL;
        return this;
    }
    
    /**
     * Устанавливает стикерсет как приватный
     */
    public StickerSetTestBuilder asPrivate() {
        this.visibility = StickerSetVisibility.PRIVATE;
        return this;
    }
    
    /**
     * Устанавливает стикерсет как заблокированный
     */
    public StickerSetTestBuilder asBlocked(String reason) {
        this.state = StickerSetState.BLOCKED;
        this.blockReason = reason;
        return this;
    }
    
    /**
     * Устанавливает стикерсет как удаленный
     */
    public StickerSetTestBuilder asDeleted() {
        this.state = StickerSetState.DELETED;
        this.deletedAt = LocalDateTime.now();
        return this;
    }
    
    /**
     * Создает объект StickerSet с установленными значениями
     */
    public StickerSet build() {
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setName(name);
        stickerSet.setTitle(title != null ? title : (name != null ? name + "_title" : "Test StickerSet"));
        stickerSet.setDescription(description);
        stickerSet.setState(state);
        stickerSet.setVisibility(visibility);
        stickerSet.setType(type);
        stickerSet.setIsVerified(Boolean.TRUE.equals(isVerified));
        stickerSet.setBlockReason(blockReason);
        stickerSet.setDeletedAt(deletedAt);
        stickerSet.setLikesCount(likesCount);
        return stickerSet;
    }
}


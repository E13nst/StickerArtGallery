package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StickerSetDto {
    
    private Long id;
    
    @NotNull(message = "ID пользователя не может быть null")
    @Positive(message = "ID пользователя должен быть положительным числом")
    private Long userId;
    
    @NotBlank(message = "Название стикерсета не может быть пустым")
    @Size(max = 64, message = "Название стикерсета не может быть длиннее 64 символов")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()]+$", message = "Название может содержать только буквы, цифры, пробелы и символы: -_.,!?()")
    private String title;
    
    @Schema(description = "Описание стикерсета (опционально)", example = "Коллекция милых котиков", nullable = true)
    private String description;
    
    @NotBlank(message = "Имя стикерсета не может быть пустым")
    @Size(min = 1, max = 64, message = "Имя стикерсета должно быть от 1 до 64 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Имя стикерсета может содержать только латинские буквы, цифры и подчеркивания")
    private String name;

    @Schema(description = "Полный URL для добавления стикерсета в Telegram", example = "https://t.me/addstickers/my_pack_by_bot")
    private String url;
    
    private LocalDateTime createdAt;
    
    @Schema(description = "Полная информация о стикерсете из Telegram Bot API (JSON объект). Может быть null, если данные недоступны.", 
            example = "{\"name\":\"my_stickers_by_StickerGalleryBot\",\"title\":\"Мои стикеры\",\"sticker_type\":\"regular\",\"is_animated\":false,\"is_video\":false,\"stickers\":[...]}", 
            nullable = true)
    private Object telegramStickerSetInfo;
    
    @Schema(description = "Список категорий стикерсета")
    private List<CategoryDto> categories;
    
    @Schema(description = "Количество лайков стикерсета", example = "42")
    private Long likesCount;
    
    @Schema(description = "Лайкнул ли текущий пользователь этот стикерсет", example = "true")
    private boolean isLikedByCurrentUser;
    
    @Schema(description = "Состояние стикерсета в жизненном цикле", example = "ACTIVE", 
            allowableValues = {"ACTIVE", "DELETED", "BLOCKED"})
    private StickerSetState state;
    
    @Schema(description = "Уровень видимости стикерсета", example = "PUBLIC", 
            allowableValues = {"PRIVATE", "PUBLIC"})
    private StickerSetVisibility visibility;
    
    @Schema(description = "Тип источника стикерсета", example = "USER", 
            allowableValues = {"USER", "OFFICIAL"})
    private StickerSetType type;
    
    @Schema(description = "Дата удаления стикерсета (только для state=DELETED)", nullable = true)
    private LocalDateTime deletedAt;
    
    @Schema(description = "Причина блокировки стикерсета (только для state=BLOCKED)", 
            example = "Нарушение правил сообщества", nullable = true)
    private String blockReason;
    
    @Deprecated
    @Schema(description = "Устаревшее поле. Используйте 'visibility'. Оставлено для обратной совместимости.", 
            example = "true", hidden = true)
    private Boolean isPublic;
    
    @Deprecated
    @Schema(description = "Устаревшее поле. Используйте 'state'. Оставлено для обратной совместимости.", 
            example = "false", hidden = true)
    private Boolean isBlocked;
    
    @Deprecated
    @Schema(description = "Устаревшее поле. Используйте 'type'. Оставлено для обратной совместимости.", 
            example = "true", hidden = true)
    private Boolean isOfficial;

    @Schema(description = "Telegram ID автора стикерсета (только отображение)", example = "123456789", nullable = true)
    private Long authorId;
    
    @Schema(description = "Список доступных действий для текущего пользователя", 
            example = "[\"DELETE\", \"UNPUBLISH\"]",
            allowableValues = {"DELETE", "BLOCK", "UNBLOCK", "PUBLISH", "UNPUBLISH"})
    private List<StickerSetAction> availableActions;
    
    // Конструкторы
    public StickerSetDto() {}
    
    public StickerSetDto(Long id, Long userId, String title, String name, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.setName(name);
        this.createdAt = createdAt;
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.url = buildUrl(name);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Object getTelegramStickerSetInfo() {
        return telegramStickerSetInfo;
    }

    public void setTelegramStickerSetInfo(Object telegramStickerSetInfo) {
        this.telegramStickerSetInfo = telegramStickerSetInfo;
    }
    
    public List<CategoryDto> getCategories() {
        return categories;
    }
    
    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
    }
    
    public Long getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }
    
    @JsonProperty("isLikedByCurrentUser")
    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }
    
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }
    
    public StickerSetState getState() {
        return state;
    }
    
    public void setState(StickerSetState state) {
        this.state = state;
    }
    
    public StickerSetVisibility getVisibility() {
        return visibility;
    }
    
    public void setVisibility(StickerSetVisibility visibility) {
        this.visibility = visibility;
    }
    
    public StickerSetType getType() {
        return type;
    }
    
    public void setType(StickerSetType type) {
        this.type = type;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public String getBlockReason() {
        return blockReason;
    }
    
    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
    
    @Deprecated
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    @Deprecated
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    @Deprecated
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    @Deprecated
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    @Deprecated
    public Boolean getIsOfficial() {
        return isOfficial;
    }
    
    @Deprecated
    public void setIsOfficial(Boolean isOfficial) {
        this.isOfficial = isOfficial;
    }
    
    public Long getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }
    
    public List<StickerSetAction> getAvailableActions() {
        return availableActions;
    }
    
    public void setAvailableActions(List<StickerSetAction> availableActions) {
        this.availableActions = availableActions;
    }
    
    /**
     * Вычисляет доступные действия для стикерсета на основе текущего пользователя, его роли и состояния стикерсета
     * 
     * @param currentUserId ID текущего пользователя (может быть null)
     * @param isAdmin является ли текущий пользователь админом
     * @param stickerSetUserId ID владельца стикерсета
     * @param stickerSetAuthorId ID автора стикерсета (может быть null)
     * @param isPublic публичный ли стикерсет
     * @param isBlocked заблокирован ли стикерсет
     * @return список доступных действий
     */
    public static List<StickerSetAction> calculateAvailableActions(
            Long currentUserId, 
            boolean isAdmin, 
            Long stickerSetUserId,
            Long stickerSetAuthorId,
            StickerSetState state,
            StickerSetVisibility visibility) {
        
        List<StickerSetAction> actions = new ArrayList<>();
        
        // Проверяем, является ли текущий пользователь владельцем
        boolean isOwner = currentUserId != null && currentUserId.equals(stickerSetUserId);
        
        // Проверяем, является ли текущий пользователь автором
        boolean isAuthor = currentUserId != null && stickerSetAuthorId != null && currentUserId.equals(stickerSetAuthorId);
        
        // DELETE - для владельца активных и заблокированных стикерсетов (не для удаленных)
        if (isOwner && state != StickerSetState.DELETED) {
            actions.add(StickerSetAction.DELETE);
        }
        
        // EDIT_CATEGORIES - для владельца и админа активных и заблокированных стикерсетов (не для удаленных)
        if (state != StickerSetState.DELETED && (isOwner || isAdmin)) {
            actions.add(StickerSetAction.EDIT_CATEGORIES);
        }
        
        // BLOCK/UNBLOCK - только для админа, показывается только одно из двух в зависимости от состояния
        if (isAdmin) {
            if (state == StickerSetState.BLOCKED) {
                actions.add(StickerSetAction.UNBLOCK);
            } else if (state == StickerSetState.ACTIVE) {
                actions.add(StickerSetAction.BLOCK);
            }
        }
        
        // PUBLISH/UNPUBLISH - для автора активных и заблокированных стикерсетов (не для удаленных)
        if (isAuthor && state != StickerSetState.DELETED) {
            if (visibility == StickerSetVisibility.PUBLIC) {
                actions.add(StickerSetAction.UNPUBLISH);
            } else {
                actions.add(StickerSetAction.PUBLISH);
            }
        }
        
        return actions;
    }
    
    @Deprecated
    public static List<StickerSetAction> calculateAvailableActions(
            Long currentUserId, 
            boolean isAdmin, 
            Long stickerSetUserId,
            Long stickerSetAuthorId,
            Boolean isPublic, 
            Boolean isBlocked) {
        // Маппинг для обратной совместимости
        StickerSetState state = Boolean.TRUE.equals(isBlocked) ? StickerSetState.BLOCKED : StickerSetState.ACTIVE;
        StickerSetVisibility visibility = Boolean.TRUE.equals(isPublic) ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE;
        return calculateAvailableActions(currentUserId, isAdmin, stickerSetUserId, stickerSetAuthorId, state, visibility);
    }
    
    // Конструктор для создания DTO из Entity
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity) {
        if (entity == null) {
            return null;
        }
        
        StickerSetDto dto = new StickerSetDto(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            entity.getName(),
            entity.getCreatedAt()
        );
        
        dto.setDescription(entity.getDescription());
        dto.setState(entity.getState());
        dto.setVisibility(entity.getVisibility());
        dto.setType(entity.getType());
        dto.setDeletedAt(entity.getDeletedAt());
        dto.setBlockReason(entity.getBlockReason());
        dto.setAuthorId(entity.getAuthorId());
        
        // Обратная совместимость для deprecated полей
        dto.setIsPublic(entity.isPublic());
        dto.setIsBlocked(entity.isBlocked());
        dto.setIsOfficial(entity.isOfficial());
        
        if (entity.getLikesCount() != null) {
            dto.setLikesCount(entity.getLikesCount().longValue());
        } else {
            dto.setLikesCount(0L);
        }
        
        // Устанавливаем пустой список действий, если не передан currentUserId
        dto.setAvailableActions(new ArrayList<>());
        
        return dto;
    }
    
    // Конструктор для создания DTO из Entity с категориями
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity, String language) {
        if (entity == null) {
            return null;
        }
        
        StickerSetDto dto = new StickerSetDto(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            entity.getName(),
            entity.getCreatedAt()
        );
        
        // Добавляем новые поля
        dto.setDescription(entity.getDescription());
        dto.setState(entity.getState());
        dto.setVisibility(entity.getVisibility());
        dto.setType(entity.getType());
        dto.setDeletedAt(entity.getDeletedAt());
        dto.setBlockReason(entity.getBlockReason());
        dto.setAuthorId(entity.getAuthorId());
        
        // Обратная совместимость для deprecated полей
        dto.setIsPublic(entity.isPublic());
        dto.setIsBlocked(entity.isBlocked());
        dto.setIsOfficial(entity.isOfficial());
        
        // Добавляем количество лайков из денормализованного поля
        if (entity.getLikesCount() != null) {
            dto.setLikesCount(entity.getLikesCount().longValue());
        } else {
            dto.setLikesCount(0L);
        }
        
        // Добавляем категории с локализацией
        if (entity.getCategories() != null && !entity.getCategories().isEmpty()) {
            dto.setCategories(
                entity.getCategories().stream()
                    .map(category -> CategoryDto.fromEntity(category, language))
                    .collect(Collectors.toList())
            );
        }
        
        // Устанавливаем isLikedByCurrentUser по умолчанию в false (будет переопределено, если передан currentUserId)
        dto.setLikedByCurrentUser(false);
        
        // Устанавливаем пустой список действий, если не передан currentUserId и isAdmin
        dto.setAvailableActions(new ArrayList<>());
        
        return dto;
    }
    
    // Конструктор для создания DTO из Entity с категориями и информацией о лайках пользователя
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity, String language, Long currentUserId) {
        return fromEntity(entity, language, currentUserId, false);
    }
    
    // Конструктор для создания DTO из Entity с категориями, информацией о лайках и доступных действиях
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity, String language, Long currentUserId, boolean isAdmin) {
        StickerSetDto dto = fromEntity(entity, language);
        
        if (dto != null && currentUserId != null) {
            dto.setLikedByCurrentUser(entity.isLikedByUser(currentUserId));
        }
        
        // Вычисляем доступные действия
        if (dto != null) {
            dto.setAvailableActions(calculateAvailableActions(
                currentUserId,
                isAdmin,
                entity.getUserId(),
                entity.getAuthorId(),
                entity.getState(),
                entity.getVisibility()
            ));
        }
        
        return dto;
    }
    
    @Override
    public String toString() {
        return "StickerSetDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    private String buildUrl(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return "https://t.me/addstickers/" + name;
    }
} 
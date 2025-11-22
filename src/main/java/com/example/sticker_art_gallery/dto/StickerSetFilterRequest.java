package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.telegram.StickerSetType;

import java.util.Set;

/**
 * DTO для параметров фильтрации стикерсетов
 * Упрощает передачу множества параметров между слоями приложения
 */
public class StickerSetFilterRequest {
    
    private PageRequest pageRequest;
    private String language;
    private Long currentUserId;
    
    // Фильтры
    private Set<String> categoryKeys;
    private StickerSetType type;
    private Long authorId;
    private boolean hasAuthorOnly;
    private Long userId;
    private boolean likedOnly;
    private boolean shortInfo;
    
    // Getters and Setters
    
    public PageRequest getPageRequest() {
        return pageRequest;
    }
    
    public void setPageRequest(PageRequest pageRequest) {
        this.pageRequest = pageRequest;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public Long getCurrentUserId() {
        return currentUserId;
    }
    
    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    public Set<String> getCategoryKeys() {
        return categoryKeys;
    }
    
    public void setCategoryKeys(Set<String> categoryKeys) {
        this.categoryKeys = categoryKeys;
    }
    
    public StickerSetType getType() {
        return type;
    }
    
    public void setType(StickerSetType type) {
        this.type = type;
    }
    
    public Long getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }
    
    public boolean isHasAuthorOnly() {
        return hasAuthorOnly;
    }
    
    public void setHasAuthorOnly(boolean hasAuthorOnly) {
        this.hasAuthorOnly = hasAuthorOnly;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public boolean isLikedOnly() {
        return likedOnly;
    }
    
    public void setLikedOnly(boolean likedOnly) {
        this.likedOnly = likedOnly;
    }
    
    public boolean isShortInfo() {
        return shortInfo;
    }
    
    public void setShortInfo(boolean shortInfo) {
        this.shortInfo = shortInfo;
    }
    
    // Методы-помощники
    
    /**
     * Проверяет наличие фильтра по категориям
     */
    public boolean hasCategoryFilter() {
        return categoryKeys != null && !categoryKeys.isEmpty();
    }
    
    /**
     * Проверяет наличие фильтра по автору
     */
    public boolean hasAuthorFilter() {
        return authorId != null || hasAuthorOnly;
    }
    
    /**
     * Проверяет наличие фильтра по пользователю
     */
    public boolean hasUserFilter() {
        return userId != null;
    }
    
    /**
     * Проверяет требование авторизации для текущего запроса
     */
    public boolean requiresAuthentication() {
        return likedOnly;
    }
    
    @Override
    public String toString() {
        return "StickerSetFilterRequest{" +
                "page=" + (pageRequest != null ? pageRequest.getPage() : "null") +
                ", size=" + (pageRequest != null ? pageRequest.getSize() : "null") +
                ", categoryKeys=" + categoryKeys +
                ", type=" + type +
                ", authorId=" + authorId +
                ", hasAuthorOnly=" + hasAuthorOnly +
                ", userId=" + userId +
                ", likedOnly=" + likedOnly +
                ", shortInfo=" + shortInfo +
                '}';
    }
}


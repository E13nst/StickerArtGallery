package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * DTO для кэширования стикеров в Redis
 */
@Data
@NoArgsConstructor
public class StickerCacheDto {
    
    /**
     * Идентификатор файла в Telegram
     */
    @JsonProperty("fileId")
    @JsonAlias({"file_id"})  // Обратная совместимость со старым форматом
    private String fileId;
    
    /**
     * Данные файла в base64
     */
    @JsonProperty("fileData")
    @JsonAlias({"file_data"})  // Обратная совместимость со старым форматом
    private String fileData;
    
    /**
     * MIME тип файла
     */
    @JsonProperty("mimeType")
    @JsonAlias({"mime_type"})  // Обратная совместимость со старым форматом
    private String mimeType;
    
    /**
     * Имя файла
     */
    @JsonProperty("fileName")
    @JsonAlias({"file_name"})  // Обратная совместимость со старым форматом
    private String fileName;
    
    /**
     * Размер файла в байтах
     */
    @JsonProperty("fileSize")
    @JsonAlias({"file_size"})  // Обратная совместимость со старым форматом
    private long fileSize;
    
    /**
     * Путь к файлу в Telegram (для отладки)
     */
    @JsonProperty("telegramFilePath")
    @JsonAlias({"telegram_file_path"})  // Обратная совместимость со старым форматом
    private String telegramFilePath;
    
    /**
     * Время последнего обновления
     */
    @JsonProperty("lastUpdated")
    @JsonAlias({"last_updated"})  // Обратная совместимость со старым форматом
    private LocalDateTime lastUpdated;
    
    /**
     * Время кеширования (алиас для lastUpdated)
     */
    @JsonIgnore
    public LocalDateTime getCachedAt() {
        return lastUpdated;
    }
    
    /**
     * Устанавливает время кеширования
     */
    @JsonIgnore
    public void setCachedAt(LocalDateTime cachedAt) {
        this.lastUpdated = cachedAt;
    }
    
    /**
     * Время истечения кеша
     */
    @JsonIgnore
    private LocalDateTime expiresAt;
    
    /**
     * Получает время истечения
     */
    @JsonIgnore
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Устанавливает время истечения
     */
    @JsonIgnore
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    @JsonCreator
    public StickerCacheDto(
            @JsonProperty("fileId") String fileId,
            @JsonProperty("fileData") String fileData,
            @JsonProperty("mimeType") String mimeType,
            @JsonProperty("fileName") String fileName,
            @JsonProperty("fileSize") long fileSize,
            @JsonProperty("telegramFilePath") String telegramFilePath,
            @JsonProperty("lastUpdated") LocalDateTime lastUpdated) {
        this.fileId = fileId;
        this.fileData = fileData;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.telegramFilePath = telegramFilePath;
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Создает StickerCacheDto из данных файла
     */
    public static StickerCacheDto create(String fileId, byte[] fileBytes, String mimeType, 
                                       String fileName, String telegramFilePath) {
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        return new StickerCacheDto(
                fileId,
                base64Data,
                mimeType,
                fileName,
                fileBytes.length,
                telegramFilePath,
                LocalDateTime.now()
        );
    }
    
    /**
     * Возвращает данные файла как byte array
     */
    @JsonIgnore
    public byte[] getFileBytes() {
        if (fileData == null) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(fileData);
    }
    
    /**
     * Алиас для getFileBytes()
     */
    @JsonIgnore
    public byte[] getData() {
        return getFileBytes();
    }
    
    /**
     * Устанавливает данные из byte array
     */
    @JsonIgnore
    public void setData(byte[] data) {
        if (data != null) {
            this.fileData = Base64.getEncoder().encodeToString(data);
            this.fileSize = data.length;
        }
    }
    
    /**
     * Алиас для getMimeType()
     */
    @JsonIgnore
    public String getContentType() {
        return mimeType;
    }
    
    /**
     * Устанавливает content type
     */
    @JsonIgnore
    public void setContentType(String contentType) {
        this.mimeType = contentType;
    }
    
    /**
     * Проверяет, не устарел ли кэш (старше 7 дней)
     */
    @JsonIgnore
    public boolean isExpired() {
        if (lastUpdated == null) {
            return true;
        }
        return lastUpdated.plusDays(7).isBefore(LocalDateTime.now());
    }
    
    @Override
    public String toString() {
        return String.format("StickerCache{fileId='%s', mimeType='%s', size=%d, updated=%s}", 
                fileId, mimeType, fileSize, lastUpdated);
    }
}

package com.example.sticker_art_gallery.service.storage;

import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.CachedImageRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для локального хранения изображений с поддержкой кэширования и автоматической очистки.
 * 
 * Изображения скачиваются с внешних URL (CloudFront), сохраняются локально
 * и отдаются через наш домен. Поддерживается lazy очистка устаревших файлов.
 */
@Service
public class ImageStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageStorageService.class);

    private final CachedImageRepository cachedImageRepository;
    private final HttpClient httpClient;

    @Value("${app.image-storage.path:./data/images}")
    private String storagePath;

    @Value("${app.image-storage.retention-days:7}")
    private int retentionDays;

    @Value("${app.image-storage.base-url:${app.url}/api/images}")
    private String baseUrl;

    public ImageStorageService(CachedImageRepository cachedImageRepository) {
        this.cachedImageRepository = cachedImageRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Инициализация директории хранилища при старте приложения.
     */
    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                LOGGER.info("📁 Создана директория хранилища изображений: {}", path.toAbsolutePath());
            }
            
            // Проверка прав на запись
            if (!Files.isWritable(path)) {
                LOGGER.error("❌ Директория хранилища не доступна для записи: {}", path.toAbsolutePath());
                throw new IllegalStateException("Image storage directory is not writable: " + path.toAbsolutePath());
            }
            
            LOGGER.info("✅ Хранилище изображений инициализировано: path={}, retention={}d, baseUrl={}",
                    path.toAbsolutePath(), retentionDays, baseUrl);
        } catch (IOException e) {
            LOGGER.error("❌ Ошибка инициализации хранилища изображений: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize image storage", e);
        }
    }

    /**
     * Скачивает изображение с внешнего URL и сохраняет локально.
     * Если изображение уже кэшировано, возвращает существующую запись.
     *
     * @param originalUrl URL изображения для скачивания
     * @return CachedImageEntity с информацией о сохраненном изображении
     */
    @Transactional
    public CachedImageEntity downloadAndStore(String originalUrl) {
        // Проверяем, есть ли уже в кэше
        Optional<CachedImageEntity> existing = cachedImageRepository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            CachedImageEntity cached = existing.get();
            // Проверяем, не истек ли срок
            if (!cached.isExpired() && Files.exists(Paths.get(storagePath, cached.getFilePath()))) {
                LOGGER.debug("📦 Изображение уже в кэше: {}", cached.getFileName());
                return cached;
            }
            // Если истек или файл отсутствует - удаляем и скачиваем заново
            deleteImage(cached);
        }

        try {
            // Скачиваем изображение
            LOGGER.info("📥 Скачивание изображения: {}...", originalUrl.substring(0, Math.min(80, originalUrl.length())));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(originalUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download image, status: " + response.statusCode());
            }

            // Определяем content-type и расширение
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("image/png");
            String extension = getExtensionFromContentType(contentType);

            // Генерируем UUID и имя файла
            UUID id = UUID.randomUUID();
            String fileName = id.toString() + "." + extension;
            String filePath = fileName; // Храним только имя файла, путь к директории в конфигурации
            
            Path fullPath = Paths.get(storagePath, filePath);
            
            // Сохраняем файл
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            long fileSize = Files.size(fullPath);
            
            // Создаем запись в БД
            CachedImageEntity entity = new CachedImageEntity();
            entity.setId(id);
            entity.setOriginalUrl(originalUrl);
            entity.setFilePath(filePath);
            entity.setFileName(fileName);
            entity.setContentType(contentType);
            entity.setFileSize(fileSize);
            entity.setExpiresAt(OffsetDateTime.now().plusDays(retentionDays));
            
            entity = cachedImageRepository.save(entity);
            
            LOGGER.info("✅ Изображение сохранено: fileName={}, size={}KB, expiresAt={}",
                    fileName, fileSize / 1024, entity.getExpiresAt());
            
            return entity;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка скачивания изображения: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download and store image: " + e.getMessage(), e);
        }
    }

    /**
     * Сохраняет готовые байты изображения в локальное хранилище.
     * Используется для провайдеров, которые возвращают бинарный результат напрямую.
     */
    @Transactional
    public CachedImageEntity storeBytes(String originalUrl, byte[] imageBytes, String contentType) {
        return storeBytes(originalUrl, imageBytes, contentType, retentionDays);
    }

    /**
     * Превью пресетов: длительное хранение (отдельно от кэша генераций).
     */
    @Transactional
    public CachedImageEntity storeStylePresetPreview(long presetId, byte[] imageBytes, String contentType) {
        String originalUrl = "style-preset-preview:" + presetId + ":" + System.currentTimeMillis();
        return storeBytes(originalUrl, imageBytes, contentType, 4000);
    }

    private CachedImageEntity storeBytes(String originalUrl, byte[] imageBytes, String contentType, long retentionDaysForImage) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes is empty");
        }
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("originalUrl is required");
        }

        Optional<CachedImageEntity> existing = cachedImageRepository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            CachedImageEntity cached = existing.get();
            if (!cached.isExpired() && Files.exists(Paths.get(storagePath, cached.getFilePath()))) {
                return cached;
            }
            deleteImage(cached);
        }

        try {
            String effectiveContentType = (contentType == null || contentType.isBlank()) ? "image/webp" : contentType;
            String extension = getExtensionFromContentType(effectiveContentType);
            UUID id = UUID.randomUUID();
            String fileName = id + "." + extension;
            Path fullPath = Paths.get(storagePath, fileName);
            Files.write(fullPath, imageBytes);

            CachedImageEntity entity = new CachedImageEntity();
            entity.setId(id);
            entity.setOriginalUrl(originalUrl);
            entity.setFilePath(fileName);
            entity.setFileName(fileName);
            entity.setContentType(effectiveContentType);
            entity.setFileSize((long) imageBytes.length);
            entity.setExpiresAt(OffsetDateTime.now().plusDays(retentionDaysForImage));
            return cachedImageRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store image bytes: " + e.getMessage(), e);
        }
    }

    /**
     * Получает изображение по ID.
     * Выполняет lazy проверку срока истечения.
     *
     * @param id UUID изображения
     * @return Optional с Resource для отдачи клиенту
     */
    @Transactional
    public Optional<ImageResource> getImage(UUID id) {
        Optional<CachedImageEntity> optionalEntity = cachedImageRepository.findById(id);
        
        if (optionalEntity.isEmpty()) {
            LOGGER.debug("🔍 Изображение не найдено: {}", id);
            return Optional.empty();
        }
        
        CachedImageEntity entity = optionalEntity.get();
        
        // Lazy проверка истечения срока
        if (entity.isExpired()) {
            LOGGER.info("⏰ Изображение просрочено, удаляем: {}", entity.getFileName());
            deleteImage(entity);
            return Optional.empty();
        }
        
        Path filePath = Paths.get(storagePath, entity.getFilePath());
        
        // Проверяем существование файла
        if (!Files.exists(filePath)) {
            LOGGER.warn("⚠️ Файл не найден на диске, удаляем запись: {}", filePath);
            cachedImageRepository.delete(entity);
            return Optional.empty();
        }
        
        try {
            Resource resource = new UrlResource(filePath.toUri());
            return Optional.of(new ImageResource(resource, entity.getContentType(), entity.getFileName()));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка чтения файла: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Получает изображение по имени файла.
     */
    @Transactional
    public Optional<ImageResource> getImageByFileName(String fileName) {
        Optional<CachedImageEntity> optionalEntity = cachedImageRepository.findByFileName(fileName);
        
        if (optionalEntity.isEmpty()) {
            return Optional.empty();
        }
        
        return getImage(optionalEntity.get().getId());
    }

    /**
     * Получает File по UUID для использования в Telegram Bot API.
     * Выполняет проверку существования файла и срока истечения.
     *
     * @param uuid UUID файла в /data/images
     * @return File если существует и не просрочен
     * @throws IllegalArgumentException если файл не найден или просрочен
     */
    @Transactional
    public java.io.File getFileByUuid(UUID uuid) {
        Optional<CachedImageEntity> optionalEntity = cachedImageRepository.findById(uuid);
        
        if (optionalEntity.isEmpty()) {
            LOGGER.warn("⚠️ Изображение не найдено в БД: {}", uuid);
            throw new IllegalArgumentException("Image not found: " + uuid);
        }
        
        CachedImageEntity entity = optionalEntity.get();
        
        // Проверка истечения срока
        if (entity.isExpired()) {
            LOGGER.info("⏰ Изображение просрочено, удаляем: {}", entity.getFileName());
            deleteImage(entity);
            throw new IllegalArgumentException("Image expired: " + uuid);
        }
        
        Path filePath = Paths.get(storagePath, entity.getFilePath());
        
        // Проверка существования файла
        if (!Files.exists(filePath)) {
            LOGGER.warn("⚠️ Файл не найден на диске, удаляем запись: {}", filePath);
            cachedImageRepository.delete(entity);
            throw new IllegalArgumentException("Image file not found: " + uuid);
        }
        
        java.io.File file = filePath.toFile();
        LOGGER.debug("✅ Файл найден: {} (size: {} bytes)", file.getAbsolutePath(), file.length());
        return file;
    }

    /**
     * Формирует публичный URL для изображения.
     *
     * @param entity кэшированное изображение
     * @return публичный URL
     */
    public String getPublicUrl(CachedImageEntity entity) {
        return baseUrl + "/" + entity.getFileName();
    }

    /**
     * Удаляет изображение из кэша и файловой системы.
     */
    @Transactional
    public void deleteImage(CachedImageEntity entity) {
        try {
            Path filePath = Paths.get(storagePath, entity.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOGGER.debug("🗑️ Файл удален: {}", filePath);
            }
        } catch (IOException e) {
            LOGGER.warn("⚠️ Не удалось удалить файл: {}", e.getMessage());
        }
        
        cachedImageRepository.delete(entity);
        LOGGER.debug("🗑️ Запись удалена из БД: {}", entity.getFileName());
    }

    /**
     * Удаляет изображение по ID.
     */
    @Transactional
    public void deleteById(UUID id) {
        cachedImageRepository.findById(id).ifPresent(this::deleteImage);
    }

    /**
     * Очищает все просроченные изображения.
     * Можно вызывать периодически через scheduled task.
     *
     * @return количество удаленных записей
     */
    @Transactional
    public int cleanupExpired() {
        LOGGER.info("🧹 Запуск очистки просроченных изображений...");
        
        var expiredImages = cachedImageRepository.findExpired(OffsetDateTime.now());
        int count = 0;
        
        for (CachedImageEntity image : expiredImages) {
            try {
                deleteImage(image);
                count++;
            } catch (Exception e) {
                LOGGER.warn("⚠️ Ошибка удаления изображения {}: {}", image.getFileName(), e.getMessage());
            }
        }
        
        LOGGER.info("🧹 Очистка завершена: удалено {} изображений", count);
        return count;
    }

    /**
     * Определяет расширение файла по Content-Type.
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return "png";
        
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> "png";
        };
    }

    /**
     * DTO для возврата изображения с метаданными.
     */
    public record ImageResource(Resource resource, String contentType, String fileName) {}
}

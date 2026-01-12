package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Контроллер для отдачи кэшированных изображений.
 * Предоставляет публичный доступ к изображениям без аутентификации.
 */
@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
@Tag(name = "Изображения", description = "API для получения кэшированных изображений")
public class ImageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageController.class);

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/{fileName}")
    @Operation(
        summary = "Получить изображение",
        description = "Возвращает кэшированное изображение по имени файла. Поддерживает форматы: png, jpg, gif, webp."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Изображение найдено и возвращено"),
        @ApiResponse(responseCode = "404", description = "Изображение не найдено"),
        @ApiResponse(responseCode = "410", description = "Изображение устарело и было удалено")
    })
    public ResponseEntity<Resource> getImage(
            @Parameter(description = "Имя файла (UUID.расширение)", example = "550e8400-e29b-41d4-a716-446655440000.png")
            @PathVariable String fileName) {

        LOGGER.debug("🔍 Запрос изображения: {}", fileName);

        // Валидация имени файла
        if (!isValidFileName(fileName)) {
            LOGGER.warn("⚠️ Некорректное имя файла: {}", fileName);
            return ResponseEntity.badRequest().build();
        }

        Optional<ImageStorageService.ImageResource> imageResource = imageStorageService.getImageByFileName(fileName);

        if (imageResource.isEmpty()) {
            LOGGER.debug("🔍 Изображение не найдено или устарело: {}", fileName);
            // Возвращаем 410 Gone для устаревших изображений
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        ImageStorageService.ImageResource resource = imageResource.get();
        MediaType mediaType = parseMediaType(resource.contentType());

        LOGGER.debug("✅ Отдаем изображение: {}, type={}", fileName, mediaType);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // Кэш на 1 день
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.fileName() + "\"")
                .body(resource.resource());
    }

    @GetMapping("/{id}.{extension}")
    @Operation(
        summary = "Получить изображение по ID и расширению",
        description = "Альтернативный endpoint для получения изображения с явным указанием расширения."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Изображение найдено и возвращено"),
        @ApiResponse(responseCode = "404", description = "Изображение не найдено"),
        @ApiResponse(responseCode = "410", description = "Изображение устарело и было удалено")
    })
    public ResponseEntity<Resource> getImageByIdAndExtension(
            @Parameter(description = "UUID изображения", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,
            @Parameter(description = "Расширение файла", example = "png")
            @PathVariable String extension) {

        String fileName = id + "." + extension;
        return getImage(fileName);
    }

    /**
     * Проверяет корректность имени файла (UUID.расширение).
     */
    private boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= fileName.length() - 1) {
            return false;
        }

        String uuidPart = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        // Проверяем расширение
        if (!isValidExtension(extension)) {
            return false;
        }

        // Проверяем UUID формат
        try {
            UUID.fromString(uuidPart);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Проверяет допустимость расширения файла.
     */
    private boolean isValidExtension(String extension) {
        return switch (extension) {
            case "png", "jpg", "jpeg", "gif", "webp" -> true;
            default -> false;
        };
    }

    /**
     * Парсит MIME-тип из строки.
     */
    private MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.IMAGE_PNG;
        }
    }
}

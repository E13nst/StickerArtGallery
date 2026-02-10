package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.*;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import com.example.sticker_art_gallery.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Контроллер для работы с данными пользователей из Telegram
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи", description = "Данные пользователей из Telegram")
@SecurityRequirement(name = "TelegramInitData")
public class UserController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    private final StatisticsService statisticsService;
    private final UserProfileService userProfileService;
    
    @Autowired
    public UserController(UserService userService,
                         StatisticsService statisticsService,
                         UserProfileService userProfileService) {
        this.userService = userService;
        this.statisticsService = statisticsService;
        this.userProfileService = userProfileService;
    }
    
    /**
     * Получить данные пользователя из Telegram по ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Получить данные пользователя из Telegram",
        description = "Возвращает данные пользователя из Telegram Bot API по его ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные пользователя получены",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 123456789,
                        "username": "testuser",
                        "firstName": "Test",
                        "lastName": "User",
                        "languageCode": "ru",
                        "isPremium": true,
                        "createdAt": "2025-10-20T10:00:00Z",
                        "updatedAt": "2025-10-20T10:00:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.debug("🔍 Получение данных пользователя по ID: {}", id);
            
            Optional<UserEntity> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            UserDto userDto = UserDto.fromEntity(userOpt.get());
            
            LOGGER.debug("✅ Данные пользователя получены: {}", userDto.getUsername());
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении данных пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить профиль пользователя по Telegram ID
     */
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Получить профиль пользователя",
        description = "Возвращает профиль пользователя по его Telegram ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Профиль найден",
            content = @Content(schema = @Schema(implementation = UserProfileDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "role": "USER",
                        "artBalance": 100,
                        "user": {
                            "id": 123456789,
                            "username": "testuser",
                            "firstName": "Test",
                            "lastName": "User",
                            "languageCode": "ru",
                            "isPremium": true,
                            "createdAt": "2025-10-20T10:00:00Z",
                            "updatedAt": "2025-10-20T10:00:00Z"
                        },
                        "createdAt": "2025-01-15T10:30:00Z",
                        "updatedAt": "2025-01-15T14:30:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> getUserProfile(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.debug("🔍 Получение профиля пользователя по Telegram ID: {}", id);
            Optional<UserProfileEntity> profileOpt = userProfileService.findByTelegramId(id);

            if (profileOpt.isPresent()) {
                UserProfileDto profileDto = UserProfileDto.fromEntity(profileOpt.get());

                Optional<UserEntity> userOpt = userService.findById(id);
                if (userOpt.isPresent()) {
                    profileDto.setUser(UserDto.fromEntity(userOpt.get()));
                }

                LOGGER.debug("✅ Профиль найден: userId={}, role={}, balance={}",
                    profileDto.getUserId(), profileDto.getRole(), profileDto.getArtBalance());
                return ResponseEntity.ok(profileDto);
            } else {
                LOGGER.warn("⚠️ Профиль пользователя с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске профиля пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить фото профиля пользователя
     */
    @GetMapping("/{id}/photo")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Получить фото профиля пользователя",
        description = "Возвращает информацию о фото профиля пользователя из Telegram Bot API с file_id для скачивания"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Фото профиля получено",
            content = @Content(schema = @Schema(implementation = java.util.Map.class),
                examples = @ExampleObject(value = """
                    {
                        "profilePhotos": {
                            "total_count": 4,
                            "photos": [[{...}]]
                        },
                        "profilePhotoFileId": "AgACAgIAAxkBAAIBY2..."
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден или нет фото"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<java.util.Map<String, Object>> getUserPhoto(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.debug("📷 Получение фото профиля пользователя: {}", id);
            
            java.util.Map<String, Object> photoData = userService.getUserProfilePhoto(id);
            if (photoData == null) {
                LOGGER.warn("⚠️ Фото профиля для пользователя {} не найдено", id);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.debug("✅ Фото профиля получено для пользователя: {}", id);
            return ResponseEntity.ok(photoData);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении фото профиля пользователя {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить статистику по пользователям
     */
    @GetMapping("/statistics")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Получить статистику по пользователям",
        description = "Возвращает статистику по пользователям: общее количество, новые за день/неделю, активные за день/неделю"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статистика получена",
            content = @Content(schema = @Schema(implementation = UserStatisticsDto.class),
                examples = @ExampleObject(value = """
                    {
                        "total": 1250,
                        "daily": 15,
                        "weekly": 98,
                        "activeDaily": 45,
                        "activeWeekly": 320
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserStatisticsDto> getUserStatistics() {
        try {
            LOGGER.debug("📊 Запрос статистики по пользователям");
            UserStatisticsDto statistics = statisticsService.getUserStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении статистики пользователей: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить рейтинг пользователей по количеству созданных стикерсетов
     */
    @GetMapping("/leaderboard")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Получить рейтинг пользователей",
        description = """
            Возвращает рейтинг пользователей по количеству созданных стикерсетов.
            Параметр visibility определяет, по какому типу стикерсетов сортировать рейтинг:
            - PUBLIC: сортировка по количеству публичных стикерсетов
            - PRIVATE: сортировка по количеству приватных стикерсетов
            - не указан: сортировка по общему количеству стикерсетов (totalCount)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Рейтинг получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "content": [
                            {
                                "userId": 123456789,
                                "username": "testuser",
                                "firstName": "Test",
                                "lastName": "User",
                                "totalCount": 42,
                                "publicCount": 28,
                                "privateCount": 14
                            }
                        ],
                        "page": 0,
                        "size": 20,
                        "totalElements": 150,
                        "totalPages": 8
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<UserLeaderboardDto>> getUserLeaderboard(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Тип видимости для сортировки рейтинга (PUBLIC/PRIVATE). Если не указан, сортировка по общему количеству (totalCount)", 
                       example = "PUBLIC", 
                       schema = @Schema(allowableValues = {"PUBLIC", "PRIVATE"}, defaultValue = "PUBLIC"))
            @RequestParam(required = false) StickerSetVisibility visibility) {
        try {
            // Если visibility не передан (null), используем null для общей статистики
            // Если передан PUBLIC или PRIVATE, используем его для соответствующей сортировки
            LOGGER.debug("🏆 Запрос рейтинга пользователей: page={}, size={}, visibility={}", page, size, visibility);
            PageResponse<UserLeaderboardDto> leaderboard = statisticsService.getUserLeaderboard(page, size, visibility);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении рейтинга пользователей: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.ArtTransactionDto;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.UpdateUserProfileRequest;
import com.example.sticker_art_gallery.dto.UserDto;
import com.example.sticker_art_gallery.dto.UserProfileDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.projection.UserProfileWithStickerCountsProjection;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Контроллер для работы с профилями пользователей
 */
@RestController
@RequestMapping("/api/profiles")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Профили пользователей", description = "Управление профилями пользователей")
@SecurityRequirement(name = "TelegramInitData")
public class UserProfileController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileController.class);
    
    private final UserProfileService userProfileService;
    private final UserService userService;
    private final ArtRewardService artRewardService;
    
    @Autowired
    public UserProfileController(UserProfileService userProfileService,
                                 UserService userService,
                                 ArtRewardService artRewardService) {
        this.userProfileService = userProfileService;
        this.userService = userService;
        this.artRewardService = artRewardService;
    }
    
    /**
     * Получить профиль пользователя по ID профиля
     */
    @GetMapping("/{profileId}")
    @Operation(
        summary = "Получить профиль по ID профиля",
        description = "Возвращает профиль пользователя по ID профиля"
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
    public ResponseEntity<UserProfileDto> getProfileById(
            @Parameter(description = "ID профиля", required = true, example = "1")
            @PathVariable Long profileId) {
        try {
            LOGGER.debug("🔍 Поиск профиля по ID профиля: {}", profileId);
            Optional<UserProfileEntity> profileOpt = userProfileService.findById(profileId);
            
            if (profileOpt.isPresent()) {
                UserProfileEntity profile = profileOpt.get();
                UserProfileDto profileDto = UserProfileDto.fromEntity(profile);
                
                // Загружаем информацию о пользователе из Telegram
                Optional<UserEntity> userOpt = userService.findById(profile.getUserId());
                if (userOpt.isPresent()) {
                    profileDto.setUser(UserDto.fromEntity(userOpt.get()));
                }
                
                LOGGER.debug("✅ Профиль найден: id={}, userId={}, role={}, balance={}", 
                           profileDto.getId(), profileDto.getUserId(), profileDto.getRole(), profileDto.getArtBalance());
                return ResponseEntity.ok(profileDto);
            } else {
                LOGGER.warn("⚠️ Профиль с ID {} не найден", profileId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске профиля с ID {}: {}", profileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить мои транзакции ART
     */
    @GetMapping("/me/transactions")
    @Operation(
        summary = "Получить мои транзакции ART",
        description = "Возвращает историю начислений и списаний ART текущего пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список транзакций получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(
                    name = "Пример списка транзакций",
                    value = """
                        {
                          "content": [
                            {
                              "id": 42,
                              "userId": 123456789,
                              "ruleCode": "UPLOAD_STICKERSET",
                              "direction": "CREDIT",
                              "delta": 10,
                              "balanceAfter": 120,
                              "metadata": "{\\"stickerSetId\\":987}",
                              "externalId": "sticker-upload:123456789:987",
                              "performedBy": 123456789,
                              "createdAt": "2025-01-15T12:00:00Z"
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1,
                          "totalPages": 1,
                          "first": true,
                          "last": true,
                          "hasNext": false,
                          "hasPrevious": false
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<ArtTransactionDto>> getMyTransactions(
            @ParameterObject @Valid PageRequest pageRequest) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            LOGGER.warn("⚠️ Попытка получить транзакции без авторизации");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return buildTransactionsResponse(currentUserId, currentUserId, pageRequest);
    }

    /**
     * Получить транзакции ART по ID профиля
     */
    @GetMapping("/{profileId}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить транзакции ART по ID профиля",
        description = "Возвращает историю начислений и списаний ART для профиля (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список транзакций получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(
                    name = "Пример списка транзакций профиля",
                    value = """
                        {
                          "content": [
                            {
                              "id": 51,
                              "userId": 123456789,
                              "ruleCode": "ADMIN_DEBIT",
                              "direction": "DEBIT",
                              "delta": -20,
                              "balanceAfter": 80,
                              "metadata": "{\\"reason\\":\\"manual_adjustment\\"}",
                              "externalId": null,
                              "performedBy": 987654321,
                              "createdAt": "2025-01-16T09:30:00Z"
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1,
                          "totalPages": 1,
                          "first": true,
                          "last": true,
                          "hasNext": false,
                          "hasPrevious": false
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<ArtTransactionDto>> getProfileTransactions(
            @Parameter(description = "ID профиля", required = true, example = "1")
            @PathVariable Long profileId,
            @ParameterObject @Valid PageRequest pageRequest) {
        Optional<UserProfileEntity> profileOpt = userProfileService.findById(profileId);
        if (profileOpt.isEmpty()) {
            LOGGER.warn("⚠️ Профиль с ID {} не найден", profileId);
            return ResponseEntity.notFound().build();
        }

        return buildTransactionsResponse(profileOpt.get().getUserId(), getCurrentUserId(), pageRequest);
    }
    
    /**
     * Обновить профиль пользователя (только для админа)
     */
    @PatchMapping("/{profileId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Обновить профиль по ID профиля (ADMIN)",
        description = "Обновляет профиль пользователя по ID профиля. Доступно только администраторам. Можно обновлять роль, баланс, статус блокировки и статус подписки."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Профиль успешно обновлен",
            content = @Content(schema = @Schema(implementation = UserProfileDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "role": "ADMIN",
                        "artBalance": 500,
                        "isBlocked": false,
                        "subscriptionStatus": "ACTIVE",
                        "user": {
                            "id": 123456789,
                            "username": "testuser",
                            "firstName": "Test",
                            "lastName": "User"
                        },
                        "createdAt": "2025-01-15T10:30:00Z",
                        "updatedAt": "2025-02-09T12:00:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @Parameter(description = "ID профиля", required = true, example = "1")
            @PathVariable Long profileId,
            @Parameter(description = "Данные для обновления профиля", required = true)
            @RequestBody @Valid UpdateUserProfileRequest request) {
        try {
            LOGGER.info("🔧 Запрос на обновление профиля {}: {}", profileId, request);

            // Обновляем профиль
            UserProfileEntity updatedProfile = userProfileService.updateProfileByProfileId(profileId, request);

            // Формируем DTO с данными пользователя
            UserProfileDto profileDto = UserProfileDto.fromEntity(updatedProfile);

            // Загружаем информацию о пользователе из Telegram
            Optional<UserEntity> userOpt = userService.findById(updatedProfile.getUserId());
            if (userOpt.isPresent()) {
                profileDto.setUser(UserDto.fromEntity(userOpt.get()));
            }

            LOGGER.info("✅ Профиль {} успешно обновлен", profileId);
            return ResponseEntity.ok(profileDto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Профиль не найден: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении профиля {}: {}", profileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить мой профиль
     */
    @GetMapping("/me")
    @Operation(
        summary = "Получить мой профиль",
        description = "Возвращает профиль текущего авторизованного пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Профиль найден"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> getMyProfile() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Не удалось определить ID текущего пользователя");
                return ResponseEntity.badRequest().build();
            }
            
            LOGGER.debug("🔍 Получение профиля текущего пользователя: {}", currentUserId);
            Optional<UserProfileEntity> profileOpt = userProfileService.findByTelegramId(currentUserId);
            
            if (profileOpt.isPresent()) {
                UserProfileDto profileDto = UserProfileDto.fromEntity(profileOpt.get());
                
                // Загружаем информацию о пользователе из Telegram
                Optional<UserEntity> userOpt = userService.findById(currentUserId);
                if (userOpt.isPresent()) {
                    profileDto.setUser(UserDto.fromEntity(userOpt.get()));
                }
                
                LOGGER.debug("✅ Профиль найден: userId={}, role={}, balance={}", 
                           profileDto.getUserId(), profileDto.getRole(), profileDto.getArtBalance());
                return ResponseEntity.ok(profileDto);
            } else {
                LOGGER.warn("⚠️ Профиль текущего пользователя {} не найден", currentUserId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении профиля текущего пользователя: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Извлечь ID текущего пользователя из SecurityContext
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке прав администратора: {}", e.getMessage());
        }
        return false;
    }

    private ResponseEntity<PageResponse<ArtTransactionDto>> buildTransactionsResponse(Long targetUserId,
                                                                                      Long requesterId,
                                                                                      PageRequest pageRequest) {
        try {
            if (targetUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            if (!Objects.equals(targetUserId, requesterId) && !isCurrentUserAdmin()) {
                LOGGER.warn("⚠️ Попытка доступа к транзакциям пользователя {} без прав. Текущий пользователь: {}", targetUserId, requesterId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            var page = artRewardService.findTransactions(targetUserId, pageRequest.toPageable());
            List<ArtTransactionDto> dtos = page.getContent().stream()
                    .map(ArtTransactionDto::fromEntity)
                    .toList();

            PageResponse<ArtTransactionDto> response = PageResponse.of(page, dtos);
            LOGGER.debug("🔍 Найдено {} транзакций ART для пользователя {}", response.getContent().size(), targetUserId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении транзакций ART для пользователя {}: {}", targetUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить список всех профилей с фильтрами и пагинацией (только для админа)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить список всех профилей (ADMIN)",
        description = "Возвращает список профилей с фильтрацией, пагинацией и сортировкой. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список профилей получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "content": [{
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
                            "isBlocked": false,
                            "createdAt": "2025-01-15T10:00:00Z",
                            "updatedAt": "2025-01-15T10:00:00Z"
                        }],
                        "page": 0,
                        "size": 20,
                        "totalElements": 150,
                        "totalPages": 8
                    }
                    """))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<UserProfileDto>> getAllProfiles(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки (createdAt, ownedStickerSetsCount, authoredStickerSetsCount)", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction,
            @Parameter(description = "Фильтр по роли (USER/ADMIN)", example = "USER")
            @RequestParam(required = false) String role,
            @Parameter(description = "Фильтр по статусу блокировки", example = "false")
            @RequestParam(required = false) Boolean isBlocked,
            @Parameter(description = "Универсальный поиск по User ID или username", example = "123456789")
            @RequestParam(required = false) String search) {
        try {
            LOGGER.debug("🔍 Получение списка профилей: page={}, size={}, sort={}, direction={}, " +
                        "role={}, isBlocked={}, search={}",
                        page, size, sort, direction, role, isBlocked, search);
            
            // Парсим роль
            UserProfileEntity.UserRole roleEnum = null;
            if (role != null && !role.trim().isEmpty()) {
                try {
                    roleEnum = UserProfileEntity.UserRole.valueOf(role.toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("⚠️ Некорректное значение роли: {}", role);
                }
            }
            
            // Валидируем и нормализуем параметры сортировки
            String validatedSort = validateSortField(sort);
            String validatedDirection = validateDirection(direction);
            
            org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(page, size);
            
            // Получаем профили с фильтрами и счетчиками стикерсетов
            org.springframework.data.domain.Page<UserProfileWithStickerCountsProjection> profilesPage = 
                userProfileService.findAllWithFiltersAndCounts(
                    roleEnum, isBlocked, search,
                    validatedSort, validatedDirection,
                    pageRequest
                );
            
            // Пакетно загружаем пользователей, чтобы избежать N+1 запросов
            List<Long> userIds = profilesPage.getContent().stream()
                    .map(UserProfileWithStickerCountsProjection::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            Map<Long, UserEntity> usersById = userService.findAllByIds(userIds);

            List<UserProfileDto> profileDtos = profilesPage.getContent().stream()
                    .map(projection -> {
                        UserProfileDto dto = new UserProfileDto();
                        dto.setId(projection.getId());
                        dto.setUserId(projection.getUserId());
                        dto.setRole(projection.getRole());
                        dto.setArtBalance(projection.getArtBalance());
                        dto.setIsBlocked(projection.getIsBlocked());
                        // Конвертируем Instant в OffsetDateTime (UTC)
                        dto.setCreatedAt(projection.getCreatedAt() != null 
                            ? java.time.OffsetDateTime.ofInstant(projection.getCreatedAt(), java.time.ZoneOffset.UTC)
                            : null);
                        dto.setUpdatedAt(projection.getUpdatedAt() != null 
                            ? java.time.OffsetDateTime.ofInstant(projection.getUpdatedAt(), java.time.ZoneOffset.UTC)
                            : null);
                        dto.setOwnedStickerSetsCount(projection.getOwnedStickerSetsCount());
                        dto.setAuthoredStickerSetsCount(projection.getAuthoredStickerSetsCount());
                        
                        UserEntity user = usersById.get(projection.getUserId());
                        if (user != null) {
                            dto.setUser(UserDto.fromEntity(user));
                        }
                        return dto;
                    })
                    .toList();
            
            PageResponse<UserProfileDto> response = PageResponse.of(profilesPage, profileDtos);
            
            LOGGER.debug("✅ Найдено {} профилей (страница {}/{})",
                        response.getTotalElements(), page + 1, response.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка профилей: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Валидирует поле сортировки и возвращает значение по умолчанию если невалидное
     */
    private String validateSortField(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "createdAt";
        }
        
        // Whitelist допустимых полей для сортировки
        String normalized = sort.trim();
        return switch (normalized) {
            case "createdAt", "ownedStickerSetsCount", "authoredStickerSetsCount" -> normalized;
            default -> {
                LOGGER.warn("⚠️ Некорректное поле сортировки: {}, используется createdAt", sort);
                yield "createdAt";
            }
        };
    }
    
    /**
     * Валидирует направление сортировки и возвращает значение по умолчанию если невалидное
     */
    private String validateDirection(String direction) {
        if (direction == null || direction.trim().isEmpty()) {
            return "DESC";
        }
        
        String normalized = direction.trim().toUpperCase();
        if ("ASC".equals(normalized) || "DESC".equals(normalized)) {
            return normalized;
        }
        
        LOGGER.warn("⚠️ Некорректное направление сортировки: {}, используется DESC", direction);
        return "DESC";
    }
}

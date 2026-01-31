package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.referral.ReferralLinkDto;
import com.example.sticker_art_gallery.service.referral.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для работы с реферальной программой
 */
@RestController
@RequestMapping("/api/referrals")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Реферальная программа", description = "API для работы с реферальными ссылками и бонусами")
@SecurityRequirement(name = "TelegramInitData")
public class ReferralController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferralController.class);

    private final ReferralService referralService;

    @Autowired
    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    /**
     * Получить реферальную ссылку текущего пользователя
     */
    @GetMapping("/me/link")
    @Operation(
        summary = "Получить мою реферальную ссылку",
        description = "Возвращает реферальную ссылку текущего пользователя для приглашения друзей. " +
                     "При первом вызове создаёт уникальный код."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Реферальная ссылка успешно получена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReferralLinkDto.class),
                examples = @ExampleObject(
                    name = "Пример реферальной ссылки",
                    value = """
                        {
                          "code": "AbC123XyZ456",
                          "startParam": "ref_AbC123XyZ456",
                          "url": "https://t.me/stixlybot?startapp=ref_AbC123XyZ456"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<ReferralLinkDto> getMyReferralLink() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Попытка получить реферальную ссылку без авторизации");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            LOGGER.debug("🔗 Получение реферальной ссылки для пользователя: {}", currentUserId);
            
            ReferralLinkDto linkDto = referralService.getOrCreateMyReferralLink(currentUserId);
            
            LOGGER.debug("✅ Реферальная ссылка получена: code={}", linkDto.getCode());
            return ResponseEntity.ok(linkDto);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении реферальной ссылки: {}", e.getMessage(), e);
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
}

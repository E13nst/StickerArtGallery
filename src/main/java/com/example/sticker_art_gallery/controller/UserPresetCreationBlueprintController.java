package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.UserPresetCreationBlueprintDto;
import com.example.sticker_art_gallery.service.generation.UserPresetCreationBlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generation/user-preset-creation-blueprints")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Создание пресета пользователем", description = "Шаблоны полей/UI для формы «свой пресет» (без хардкода на фронте)")
@SecurityRequirement(name = "TelegramInitData")
public class UserPresetCreationBlueprintController {

    private final UserPresetCreationBlueprintService blueprintService;

    @Autowired
    public UserPresetCreationBlueprintController(UserPresetCreationBlueprintService blueprintService) {
        this.blueprintService = blueprintService;
    }

    @GetMapping
    @Operation(summary = "Активные шаблоны создания пользовательских пресетов",
            description = "Слить presetDefaults в POST /api/generation/style-presets вместе с code/name/categoryId. " +
                    "estimatedPublicationCostArt — подсказка по правилу PUBLISH_PRESET.")
    public ResponseEntity<List<UserPresetCreationBlueprintDto>> list() {
        if (extractUserIdFromAuthentication() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(blueprintService.listEnabledForUser());
    }

    private Long extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

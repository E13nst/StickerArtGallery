package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.UpsertUserPresetCreationBlueprintRequest;
import com.example.sticker_art_gallery.dto.generation.UserPresetCreationBlueprintDto;
import com.example.sticker_art_gallery.service.generation.UserPresetCreationBlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user-preset-creation-blueprints")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: шаблоны создания пресета", description = "CRUD шаблонов формы «создать свой пресет» для мини-приложения")
@SecurityRequirement(name = "TelegramInitData")
public class UserPresetCreationBlueprintAdminController {

    private final UserPresetCreationBlueprintService blueprintService;

    @Autowired
    public UserPresetCreationBlueprintAdminController(UserPresetCreationBlueprintService blueprintService) {
        this.blueprintService = blueprintService;
    }

    @GetMapping
    @Operation(summary = "Список шаблонов (включая выключенные)")
    public ResponseEntity<List<UserPresetCreationBlueprintDto>> listAll() {
        return ResponseEntity.ok(blueprintService.listAllForAdmin());
    }

    @PostMapping
    @Operation(summary = "Добавить шаблон")
    public ResponseEntity<UserPresetCreationBlueprintDto> create(@Valid @RequestBody UpsertUserPresetCreationBlueprintRequest request) {
        try {
            return ResponseEntity.ok(blueprintService.create(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить шаблон по id")
    public ResponseEntity<UserPresetCreationBlueprintDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertUserPresetCreationBlueprintRequest request) {
        try {
            return ResponseEntity.ok(blueprintService.update(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить шаблон")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            blueprintService.delete(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.ArtRuleDto;
import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/art-rules")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "ART Правила", description = "Управление правилами начисления и списания ART")
@SecurityRequirement(name = "TelegramInitData")
public class ArtRuleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtRuleController.class);

    private final ArtRuleService artRuleService;

    public ArtRuleController(ArtRuleService artRuleService) {
        this.artRuleService = artRuleService;
    }

    @GetMapping
    @Operation(summary = "Список правил ART", description = "Возвращает все правила начисления и списания ART")
    @ApiResponse(
            responseCode = "200",
            description = "Правила получены",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ArtRuleDto.class),
                    examples = @ExampleObject(
                            name = "Пример списка правил",
                            value = """
                                [
                                  {
                                    "id": 1,
                                    "code": "UPLOAD_STICKERSET",
                                    "direction": "CREDIT",
                                    "amount": 10,
                                    "isEnabled": true,
                                    "description": "Начисление за загрузку стикерсета",
                                    "metadataSchema": "{\\"type\\":\\"object\\",\\"required\\":[\\"stickerSetId\\"]}"
                                  }
                                ]
                                """
                    )
            )
    )
    public ResponseEntity<List<ArtRuleDto>> getRules() {
        List<ArtRuleDto> rules = artRuleService.getAllRules().stream()
                .map(ArtRuleDto::fromEntity)
                .collect(Collectors.toList());
        LOGGER.info("🔍 Получено {} правил ART", rules.size());
        return ResponseEntity.ok(rules);
    }

    @PostMapping
    @Operation(summary = "Создать правило ART", description = "Создает новое правило начисления или списания ART")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Правило создано",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ArtRuleDto.class),
                            examples = @ExampleObject(
                                    name = "Созданное правило",
                                    value = """
                                        {
                                          "id": 7,
                                          "code": "DAILY_LOGIN",
                                          "direction": "CREDIT",
                                          "amount": 3,
                                          "isEnabled": true,
                                          "description": "Начисление за ежедневный вход",
                                          "metadataSchema": null,
                                          "createdAt": "2025-01-16T11:00:00Z",
                                          "updatedAt": "2025-01-16T11:00:00Z"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "Правило с таким кодом уже существует"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<ArtRuleDto> createRule(@Valid @RequestBody ArtRuleDto dto) {
        try {
            if (artRuleService.findByCode(dto.getCode()).isPresent()) {
                LOGGER.warn("⚠️ Правило ART с кодом {} уже существует", dto.getCode());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            ArtRuleEntity entity = dto.toEntity();
            entity.setId(null);
            entity.setCreatedAt(null);
            entity.setUpdatedAt(null);

            ArtRuleEntity saved = artRuleService.save(entity);
            LOGGER.info("✅ Создано правило ART: code={}", saved.getCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(ArtRuleDto.fromEntity(saved));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании правила ART: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{code}")
    @Operation(summary = "Обновить правило ART", description = "Обновляет существующее правило начисления или списания ART")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Правило обновлено",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ArtRuleDto.class),
                            examples = @ExampleObject(
                                    name = "Обновлённое правило",
                                    value = """
                                        {
                                          "id": 1,
                                          "code": "UPLOAD_STICKERSET",
                                          "direction": "CREDIT",
                                          "amount": 15,
                                          "isEnabled": true,
                                          "description": "Начисление за загрузку стикерсета (повышенный бонус)",
                                          "metadataSchema": "{\\"type\\":\\"object\\",\\"required\\":[\\"stickerSetId\\"]}",
                                          "createdAt": "2025-01-10T09:00:00Z",
                                          "updatedAt": "2025-01-16T12:15:00Z"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Правило не найдено"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<ArtRuleDto> updateRule(
            @Parameter(description = "Код правила ART", required = true, example = "UPLOAD_STICKERSET")
            @PathVariable String code,
            @Valid @RequestBody ArtRuleDto dto) {
        try {
            ArtRuleEntity existing = artRuleService.findByCode(code)
                    .orElse(null);
            if (existing == null) {
                LOGGER.warn("⚠️ Правило ART с кодом {} не найдено", code);
                return ResponseEntity.notFound().build();
            }

            existing.setDirection(dto.getDirection());
            existing.setAmount(dto.getAmount());
            existing.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : existing.getIsEnabled());
            existing.setDescription(dto.getDescription());
            existing.setMetadataSchema(dto.getMetadataSchema());

            ArtRuleEntity saved = artRuleService.save(existing);
            LOGGER.info("♻️ Обновлено правило ART: code={}", code);
            return ResponseEntity.ok(ArtRuleDto.fromEntity(saved));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении правила ART {}: {}", code, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}


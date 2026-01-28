package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.payment.StarsPackageDto;
import com.example.sticker_art_gallery.dto.payment.StarsPurchaseDto;
import com.example.sticker_art_gallery.model.payment.StarsPackageEntity;
import com.example.sticker_art_gallery.repository.StarsPackageRepository;
import com.example.sticker_art_gallery.repository.StarsPurchaseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Админ контроллер для управления тарифными пакетами Stars
 */
@RestController
@RequestMapping("/api/admin/stars/packages")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Stars Packages (Admin)", description = "Управление тарифными пакетами Stars (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class StarsPackageAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarsPackageAdminController.class);

    private final StarsPackageRepository starsPackageRepository;
    private final StarsPurchaseRepository starsPurchaseRepository;

    @Autowired
    public StarsPackageAdminController(StarsPackageRepository starsPackageRepository,
                                       StarsPurchaseRepository starsPurchaseRepository) {
        this.starsPackageRepository = starsPackageRepository;
        this.starsPurchaseRepository = starsPurchaseRepository;
    }

    /**
     * Получить все пакеты (включая отключенные)
     */
    @GetMapping
    @Operation(
            summary = "Получить все пакеты",
            description = "Возвращает все тарифные пакеты, включая отключенные"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пакетов получен",
                    content = @Content(schema = @Schema(implementation = StarsPackageDto.class))
            )
    })
    public ResponseEntity<List<StarsPackageDto>> getAllPackages() {
        try {
            List<StarsPackageEntity> packages = starsPackageRepository.findAll();
            List<StarsPackageDto> dtos = packages.stream()
                    .map(StarsPackageDto::fromEntity)
                    .toList();
            LOGGER.info("📦 Возвращено {} пакетов (включая отключенные)", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка пакетов: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Создать новый пакет
     */
    @PostMapping
    @Operation(
            summary = "Создать новый пакет",
            description = "Создает новый тарифный пакет для покупки ART за Stars"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Пакет создан",
                    content = @Content(schema = @Schema(implementation = StarsPackageDto.class))
            ),
            @ApiResponse(responseCode = "409", description = "Пакет с таким кодом уже существует"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StarsPackageDto> createPackage(@Valid @RequestBody StarsPackageDto dto) {
        try {
            Optional<StarsPackageEntity> existing = starsPackageRepository.findByCode(dto.getCode());
            if (existing.isPresent()) {
                LOGGER.warn("⚠️ Пакет с кодом {} уже существует", dto.getCode());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            StarsPackageEntity entity = new StarsPackageEntity();
            entity.setCode(dto.getCode());
            entity.setName(dto.getName());
            entity.setDescription(dto.getDescription());
            entity.setStarsPrice(dto.getStarsPrice());
            entity.setArtAmount(dto.getArtAmount());
            entity.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : Boolean.TRUE);
            entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

            StarsPackageEntity saved = starsPackageRepository.save(entity);
            LOGGER.info("✅ Создан пакет: code={}, name={}", saved.getCode(), saved.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(StarsPackageDto.fromEntity(saved));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании пакета: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Обновить пакет
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Обновить пакет",
            description = "Обновляет существующий тарифный пакет"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пакет обновлен",
                    content = @Content(schema = @Schema(implementation = StarsPackageDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Пакет не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StarsPackageDto> updatePackage(
            @Parameter(description = "ID пакета", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody StarsPackageDto dto) {
        try {
            Optional<StarsPackageEntity> existingOpt = starsPackageRepository.findById(id);
            if (existingOpt.isEmpty()) {
                LOGGER.warn("⚠️ Пакет с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }

            StarsPackageEntity existing = existingOpt.get();
            existing.setName(dto.getName());
            existing.setDescription(dto.getDescription());
            existing.setStarsPrice(dto.getStarsPrice());
            existing.setArtAmount(dto.getArtAmount());
            if (dto.getIsEnabled() != null) {
                existing.setIsEnabled(dto.getIsEnabled());
            }
            if (dto.getSortOrder() != null) {
                existing.setSortOrder(dto.getSortOrder());
            }

            StarsPackageEntity saved = starsPackageRepository.save(existing);
            LOGGER.info("♻️ Обновлен пакет: id={}, code={}", saved.getId(), saved.getCode());
            return ResponseEntity.ok(StarsPackageDto.fromEntity(saved));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении пакета {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Включить/выключить пакет
     */
    @PatchMapping("/{id}/toggle")
    @Operation(
            summary = "Включить/выключить пакет",
            description = "Переключает статус активности пакета"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус пакета изменен",
                    content = @Content(schema = @Schema(implementation = StarsPackageDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Пакет не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StarsPackageDto> togglePackage(
            @Parameter(description = "ID пакета", required = true, example = "1")
            @PathVariable Long id) {
        try {
            Optional<StarsPackageEntity> existingOpt = starsPackageRepository.findById(id);
            if (existingOpt.isEmpty()) {
                LOGGER.warn("⚠️ Пакет с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }

            StarsPackageEntity existing = existingOpt.get();
            existing.setIsEnabled(!existing.getIsEnabled());

            StarsPackageEntity saved = starsPackageRepository.save(existing);
            LOGGER.info("🔄 Статус пакета изменен: id={}, code={}, enabled={}", 
                    saved.getId(), saved.getCode(), saved.getIsEnabled());
            return ResponseEntity.ok(StarsPackageDto.fromEntity(saved));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при переключении статуса пакета {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить все покупки с фильтрами
     */
    @GetMapping("/purchases")
    @Operation(
            summary = "Получить все покупки",
            description = "Возвращает все покупки ART за Stars с пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список покупок получен",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            )
    })
    public ResponseEntity<PageResponse<StarsPurchaseDto>> getAllPurchases(
            @ParameterObject @Valid PageRequest pageRequest) {
        try {
            Page<com.example.sticker_art_gallery.model.payment.StarsPurchaseEntity> purchases = 
                    starsPurchaseRepository.findAll(pageRequest.toPageable());
            PageResponse<StarsPurchaseDto> response = PageResponse.of(
                    purchases.map(StarsPurchaseDto::fromEntity)
            );
            LOGGER.info("📜 Возвращено {} покупок", response.getContent().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка покупок: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

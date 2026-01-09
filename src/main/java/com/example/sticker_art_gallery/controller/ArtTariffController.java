package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.ArtTariffItem;
import com.example.sticker_art_gallery.dto.ArtTariffsResponse;
import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionDirection;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/art-tariffs")
@Tag(name = "ART Тарифы", description = "Публичная информация о тарифах начисления и списания ART")
@SecurityRequirement(name = "TelegramInitData")
public class ArtTariffController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtTariffController.class);

    private final ArtRuleService artRuleService;

    public ArtTariffController(ArtRuleService artRuleService) {
        this.artRuleService = artRuleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Получить тарифы ART",
        description = "Возвращает все активные правила начисления и списания ART, сгруппированные по направлениям. " +
                      "Доступно для пользователей и администраторов."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Тарифы успешно получены",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ArtTariffsResponse.class),
            examples = @ExampleObject(
                name = "Пример ответа",
                value = """
                    {
                      "credits": [
                        {
                          "code": "UPLOAD_STICKERSET",
                          "amount": 10,
                          "description": "Начисление за загрузку стикерсета"
                        }
                      ],
                      "debits": [
                        {
                          "code": "GENERATE_STICKER",
                          "amount": 10,
                          "description": "Списание ART за генерацию стикера"
                        }
                      ]
                    }
                    """
            )
        )
    )
    public ResponseEntity<ArtTariffsResponse> getTariffs() {
        List<ArtRuleEntity> allRules = artRuleService.getAllRules();
        
        // Фильтруем только активные правила
        List<ArtRuleEntity> enabledRules = allRules.stream()
            .filter(ArtRuleEntity::getIsEnabled)
            .collect(Collectors.toList());
        
        // Группируем по направлениям
        List<ArtTariffItem> credits = enabledRules.stream()
            .filter(r -> r.getDirection() == ArtTransactionDirection.CREDIT)
            .map(this::toTariffItem)
            .collect(Collectors.toList());
        
        List<ArtTariffItem> debits = enabledRules.stream()
            .filter(r -> r.getDirection() == ArtTransactionDirection.DEBIT)
            .map(this::toTariffItem)
            .collect(Collectors.toList());
        
        LOGGER.debug("Получено тарифов: {} начислений, {} списаний", credits.size(), debits.size());
        
        ArtTariffsResponse response = new ArtTariffsResponse(credits, debits);
        return ResponseEntity.ok(response);
    }
    
    private ArtTariffItem toTariffItem(ArtRuleEntity rule) {
        return new ArtTariffItem(
            rule.getCode(),
            rule.getAmount(),
            rule.getDescription()
        );
    }
}

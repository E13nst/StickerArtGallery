package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.AuthorLeaderboardDto;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с авторами стикерсетов
 */
@RestController
@RequestMapping("/api/authors")
@PreAuthorize("permitAll()")
@Tag(name = "Авторы", description = "Данные об авторах стикерсетов")
public class AuthorController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorController.class);
    
    private final StatisticsService statisticsService;
    
    @Autowired
    public AuthorController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Получить рейтинг авторов по количеству созданных стикерсетов
     */
    @GetMapping("/leaderboard")
    @Operation(
        summary = "Получить рейтинг авторов",
        description = """
            Возвращает рейтинг авторов стикерсетов по количеству созданных стикерсетов.
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
                                "authorId": 123456789,
                                "username": "testauthor",
                                "firstName": "Test",
                                "lastName": "Author",
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
    public ResponseEntity<PageResponse<AuthorLeaderboardDto>> getAuthorLeaderboard(
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
            LOGGER.debug("🏆 Запрос рейтинга авторов: page={}, size={}, visibility={}", page, size, visibility);
            PageResponse<AuthorLeaderboardDto> leaderboard = statisticsService.getAuthorLeaderboard(page, size, visibility);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении рейтинга авторов: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}


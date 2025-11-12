package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StatisticsDto;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Статистика сервиса", description = "Агрегированные показатели для главной страницы")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    @Operation(summary = "Получить сводную статистику", description = "Возвращает ключевые показатели сервиса за все время и за последние сутки/неделю.")
    public StatisticsDto getStatistics() {
        return statisticsService.getStatistics();
    }
}



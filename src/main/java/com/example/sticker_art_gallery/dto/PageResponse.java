package com.example.sticker_art_gallery.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO для ответа с пагинацией
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с данными и информацией о пагинации")
public class PageResponse<T> {
    
    @Schema(description = "Данные текущей страницы")
    private List<T> content;
    
    @Schema(description = "Номер текущей страницы (начиная с 0)", example = "0")
    private int page;
    
    @Schema(description = "Размер страницы", example = "20")
    private int size;
    
    @Schema(description = "Общее количество элементов", example = "156")
    private long totalElements;
    
    @Schema(description = "Общее количество страниц", example = "8")
    private int totalPages;
    
    @Schema(description = "Является ли текущая страница первой", example = "true")
    private boolean first;
    
    @Schema(description = "Является ли текущая страница последней", example = "false")
    private boolean last;
    
    @Schema(description = "Есть ли следующая страница", example = "true")
    private boolean hasNext;
    
    @Schema(description = "Есть ли предыдущая страница", example = "false")
    private boolean hasPrevious;
    
    /**
     * Создает PageResponse из Spring Data Page
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
    
    /**
     * Создает PageResponse с преобразованием типа данных
     */
    public static <T, R> PageResponse<R> of(Page<T> page, List<R> mappedContent) {
        return new PageResponse<>(
            mappedContent,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}

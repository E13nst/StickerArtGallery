package com.example.sticker_art_gallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для запроса с пагинацией
 */
@Data
@Schema(description = "Параметры пагинации для запросов")
public class PageRequest {
    
    @Min(value = 0, message = "Номер страницы не может быть отрицательным")
    @Schema(description = "Номер страницы (начиная с 0)", example = "0", defaultValue = "0")
    private int page = 0;
    
    @Min(value = 1, message = "Размер страницы должен быть больше 0")
    @Max(value = 100, message = "Размер страницы не может быть больше 100")
    @Schema(description = "Количество элементов на странице", example = "20", defaultValue = "20")
    private int size = 20;
    
    @Schema(description = "Поле для сортировки", example = "createdAt", defaultValue = "createdAt")
    private String sort = "createdAt";
    
    @Pattern(regexp = "ASC|DESC", message = "Направление сортировки должно быть ASC или DESC")
    @Schema(description = "Направление сортировки", example = "DESC", defaultValue = "DESC", allowableValues = {"ASC", "DESC"})
    private String direction = "DESC";
    
    /**
     * Создает объект Pageable для Spring Data
     */
    public org.springframework.data.domain.Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(
            page, 
            size, 
            org.springframework.data.domain.Sort.Direction.fromString(direction), 
            sort
        );
    }
}

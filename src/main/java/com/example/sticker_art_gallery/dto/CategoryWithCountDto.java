package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Категория с количеством стикерсетов")
public class CategoryWithCountDto {

    @Schema(description = "Ключ категории", example = "animals")
    private String key;

    @Schema(description = "Локализованное название категории", example = "Животные")
    private String name;

    @Schema(description = "Количество стикерсетов в категории", example = "42")
    private Long count;

    public CategoryWithCountDto() {}

    public CategoryWithCountDto(String key, String name, Long count) {
        this.key = key;
        this.name = name;
        this.count = count;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}



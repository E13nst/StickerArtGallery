package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Частичное обновление пользовательского пресета модератором (имя, категория).
 */
@Schema(description = "Обновление отображаемого имени и/или категории пользовательского пресета (админ). "
        + "Поле со значением null или отсутствующее ключом в JSON не меняет текущее значение.")
public class AdminUserPresetModerationPatchDto {

    @Schema(description = "Публичное имя пресета (поле name в каталоге и карточке)")
    @Size(max = 100, message = "name не длиннее 100 символов")
    private String name;

    @Schema(description = "ID категории пресета; null — не менять категорию")
    private Long categoryId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public boolean hasNamePatch() {
        return name != null;
    }

    public boolean hasCategoryPatch() {
        return categoryId != null;
    }

    public void validatePresent() {
        if (!hasNamePatch() && !hasCategoryPatch()) {
            throw new IllegalArgumentException("Укажите name и/или categoryId для изменения");
        }
        if (hasNamePatch()) {
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("name не может быть пустым");
            }
            if (name.trim().length() > 100) {
                throw new IllegalArgumentException("name не может быть длиннее 100 символов");
            }
        }
    }

    public String trimmedNameOrNull() {
        if (!hasNamePatch()) {
            return null;
        }
        return name.trim();
    }
}

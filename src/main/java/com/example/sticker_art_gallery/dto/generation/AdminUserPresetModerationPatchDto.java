package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Частичное обновление пользовательского пресета модератором (имя, категория, порядок, сохранённый финальный промпт).
 */
@Schema(description = "Обновление полей пользовательского пресета (админ). "
        + "Поле со значением null или отсутствующее ключом в JSON не меняет текущее значение.")
public class AdminUserPresetModerationPatchDto {

    @Schema(description = "Публичное имя пресета (поле name в каталоге и карточке)")
    @Size(max = 100, message = "name не длиннее 100 символов")
    private String name;

    @Schema(description = "ID категории пресета; null — не менять категорию")
    private Long categoryId;

    @Schema(description = "Порядок отображения пресета внутри категории; null — не менять")
    private Integer sortOrder;

    @Schema(description = "Сохранённый финальный пользовательский промпт из miniapp (submittedUserPrompt)")
    @Size(max = 8192, message = "submittedUserPrompt не длиннее 8192 символов")
    private String submittedUserPrompt;

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean hasSortOrderPatch() {
        return sortOrder != null;
    }

    public String getSubmittedUserPrompt() {
        return submittedUserPrompt;
    }

    public void setSubmittedUserPrompt(String submittedUserPrompt) {
        this.submittedUserPrompt = submittedUserPrompt;
    }

    public boolean hasSubmittedUserPromptPatch() {
        return submittedUserPrompt != null;
    }

    public void validatePresent() {
        if (!hasNamePatch() && !hasCategoryPatch() && !hasSortOrderPatch() && !hasSubmittedUserPromptPatch()) {
            throw new IllegalArgumentException("Укажите хотя бы одно из полей: name, categoryId, sortOrder, submittedUserPrompt");
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

    public String normalizedSubmittedUserPromptOrNull() {
        if (!hasSubmittedUserPromptPatch()) {
            return null;
        }
        String trimmed = submittedUserPrompt.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 8192) {
            throw new IllegalArgumentException("submittedUserPrompt не может быть длиннее 8192 символов");
        }
        return trimmed;
    }
}

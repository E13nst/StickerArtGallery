package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Публикация пользовательского стиля после успешной генерации v2 (без предварительного черновика в {@code style_presets}).
 */
@Schema(
        description = """
                Запрос публикации стиля из завершённой задачи генерации v2.
                Канонические имена полей в camelCase (как в ответах API и в Swagger).
                Временно также принимаются snake_case ключи для обратной совместимости; предпочитайте camelCase — snake_case может быть выключён после миграции клиентов.
                """
)
public class PublishUserStyleFromTaskRequest {

    @Schema(description = "Код пресета в каталоге (уникальный для владельца)", requiredMode = Schema.RequiredMode.REQUIRED, example = "my_watercolor_v1")
    @NotBlank(message = "code не может быть пустым")
    @Size(max = 50)
    private String code;

    @Schema(description = "Публичное имя стиля (альтернативное имя JSON: display_name)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "displayName не может быть пустым")
    @JsonAlias({"display_name"})
    @Size(max = 100)
    private String displayName;

    @Schema(description = "Описание для каталога")
    private String description;

    @Schema(description = "ID категории; null — общая категория приложения (альтернатива JSON: category_id)")
    @JsonAlias({"category_id"})
    private Long categoryId;

    @Schema(description = "Порядок сортировки (альтернатива JSON: sort_order)")
    @JsonAlias({"sort_order"})
    private Integer sortOrder;

    @Schema(description = """
            Должен совпадать с metadata задачи при передаче (перекрёстная проверка).
            Каноническое имя в JSON — userStyleBlueprintCode; временно поддерживается user_style_blueprint_code.
            """
    )
    @JsonAlias({"user_style_blueprint_code"})
    @Size(max = 64)
    private String userStyleBlueprintCode;

    @Schema(description = "Клиентский UUID идемпотентности (альтернатива JSON: idempotency_key)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "idempotencyKey не может быть пустым")
    @JsonAlias({"idempotency_key"})
    @Size(max = 128)
    private String idempotencyKey;

    @Schema(description = "Явное согласие на публичный показ результата (альтернатива JSON: consent_result_public_show)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "consentResultPublicShow обязателен")
    @JsonAlias({"consent_result_public_show"})
    private Boolean consentResultPublicShow;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getUserStyleBlueprintCode() {
        return userStyleBlueprintCode;
    }

    public void setUserStyleBlueprintCode(String userStyleBlueprintCode) {
        this.userStyleBlueprintCode = userStyleBlueprintCode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Boolean getConsentResultPublicShow() {
        return consentResultPublicShow;
    }

    public void setConsentResultPublicShow(Boolean consentResultPublicShow) {
        this.consentResultPublicShow = consentResultPublicShow;
    }
}

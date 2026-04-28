-- Шаблоны для формы «создать свой пресет» (настройка в админке, без хардкода во фронте)

CREATE TABLE user_preset_creation_blueprints (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    admin_title VARCHAR(200) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    preset_defaults_json JSONB NOT NULL,
    ui_hints_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_preset_creation_blueprints_code UNIQUE (code)
);

CREATE INDEX idx_user_preset_creation_blueprints_enabled_sort
    ON user_preset_creation_blueprints (is_enabled, sort_order);

COMMENT ON TABLE user_preset_creation_blueprints IS 'Шаблоны полей/шаблона для создания пользователем персональных пресетов (слияние defaults + код/имя в POST)';
COMMENT ON COLUMN user_preset_creation_blueprints.preset_defaults_json IS 'Частичный CreateStylePresetRequest: uiMode, promptSuffix, promptInput, fields, removeBackgroundMode категории и пр.';

INSERT INTO user_preset_creation_blueprints (
    code,
    admin_title,
    is_enabled,
    sort_order,
    preset_defaults_json,
    ui_hints_json,
    created_at,
    updated_at
) VALUES (
    'style_anchor_standard',
    'Референс пресета (сервер) + фото пользователя под генерацию + промпт',
    TRUE,
    0,
    '{
      "promptSuffix": "Идея стикера: {{prompt}}. Опорное изображение стиля пресета: {{preset_ref}}. Свежий эталон снимка пользователя под эту генерацию: {{user_face}}.",
      "uiMode": "STRUCTURED_FIELDS",
      "promptInput": {
        "enabled": true,
        "required": true,
        "placeholder": "Опишите, что должно получиться на стикере…",
        "maxLength": 800
      },
      "fields": [
        {
          "key": "user_face",
          "label": "Фото только для этой генерации (можно менять каждый раз)",
          "type": "reference",
          "required": false,
          "minImages": 0,
          "maxImages": 1,
          "promptTemplate": "Image {index}"
        }
      ],
      "removeBackgroundMode": "PRESET_DEFAULT"
    }'::jsonb,
    '{
      "presetReferenceHelp": "Это фото загружается один раз: мы сохраняем его на сервере. Оно станет опорой стиля для всех следующих генераций с этим пресетом.",
      "userPhotoSlotHelp": "Сюда можно подставлять другое фото под каждую генерацию — оно не фиксируется в пресете, а используется только в текущем запуске.",
      "publicationHint": "После генерации вы можете опубликовать пресет в каталог. В карточке каталога превью обычно берётся из последнего результата генерации. Стоимость публикации списывается по правилу PUBLISH_PRESET (см. поле estimatedPublicationCostArt в ответе API)."
    }'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

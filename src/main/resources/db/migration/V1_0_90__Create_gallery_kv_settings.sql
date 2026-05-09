-- Простые key-value настройки для админки (без пересборки/деплоя env).

CREATE TABLE gallery_kv_settings (
    setting_key   VARCHAR(128) PRIMARY KEY,
    setting_value TEXT         NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE gallery_kv_settings IS 'Key-value runtime-настройки галереи (редактируются через админ-API)';
COMMENT ON COLUMN gallery_kv_settings.setting_key IS 'Уникальный ключ настройки';
COMMENT ON COLUMN gallery_kv_settings.setting_value IS 'Строковое значение (семантика зависит от ключа)';
COMMENT ON COLUMN gallery_kv_settings.updated_at IS 'Время последнего изменения';

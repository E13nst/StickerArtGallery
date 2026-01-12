-- Миграция: создание таблицы для кэшированных изображений
-- Версия: 1.0.37
-- Описание:
--   Создание таблицы cached_images для хранения метаданных локально сохраненных изображений
--   с поддержкой автоматической очистки по времени истечения

-- ============================================================================
-- 1. Таблица cached_images - метаданные кэшированных изображений
-- ============================================================================
CREATE TABLE cached_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_url TEXT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'image/png',
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);

-- Индексы для быстрого поиска и очистки
CREATE INDEX idx_cached_images_expires_at ON cached_images(expires_at);
CREATE INDEX idx_cached_images_file_name ON cached_images(file_name);
CREATE UNIQUE INDEX idx_cached_images_original_url ON cached_images(original_url);

-- Комментарии
COMMENT ON TABLE cached_images IS 'Таблица для хранения метаданных локально кэшированных изображений';
COMMENT ON COLUMN cached_images.id IS 'Уникальный идентификатор (UUID), используется в URL';
COMMENT ON COLUMN cached_images.original_url IS 'Оригинальный URL изображения (CloudFront)';
COMMENT ON COLUMN cached_images.file_path IS 'Относительный путь к файлу в локальном хранилище';
COMMENT ON COLUMN cached_images.file_name IS 'Имя файла (id.ext)';
COMMENT ON COLUMN cached_images.content_type IS 'MIME-тип файла (image/png, image/jpeg и т.д.)';
COMMENT ON COLUMN cached_images.file_size IS 'Размер файла в байтах';
COMMENT ON COLUMN cached_images.created_at IS 'Время создания записи';
COMMENT ON COLUMN cached_images.expires_at IS 'Время истечения (для автоматической очистки)';

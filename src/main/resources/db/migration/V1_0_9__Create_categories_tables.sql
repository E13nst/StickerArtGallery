-- Создание таблицы категорий
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(50) UNIQUE NOT NULL,
    name_ru VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    description_ru TEXT,
    description_en TEXT,
    icon_url VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для categories
CREATE INDEX idx_categories_key ON categories(key);
CREATE INDEX idx_categories_active ON categories(is_active);
CREATE INDEX idx_categories_display_order ON categories(display_order);

-- Создание промежуточной таблицы для связи many-to-many
CREATE TABLE sticker_set_categories (
    id BIGSERIAL PRIMARY KEY,
    sticker_set_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sticker_set_categories_sticker_set FOREIGN KEY (sticker_set_id) REFERENCES stickersets(id) ON DELETE CASCADE,
    CONSTRAINT fk_sticker_set_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT uq_sticker_set_category UNIQUE(sticker_set_id, category_id)
);

-- Индексы для sticker_set_categories
CREATE INDEX idx_sticker_set_categories_sticker_set ON sticker_set_categories(sticker_set_id);
CREATE INDEX idx_sticker_set_categories_category ON sticker_set_categories(category_id);

-- Вставка предустановленных категорий
INSERT INTO categories (key, name_ru, name_en, description_ru, description_en, display_order) VALUES
('animals', 'Животные', 'Animals', 'Стикеры с животными', 'Stickers with animals', 1),
('memes', 'Мемы', 'Memes', 'Популярные мемы', 'Popular memes', 2),
('emotions', 'Эмоции', 'Emotions', 'Выражение эмоций', 'Express emotions', 3),
('cute', 'Милые', 'Cute', 'Милые и забавные стикеры', 'Cute and funny stickers', 4),
('anime', 'Аниме', 'Anime', 'Аниме персонажи', 'Anime characters', 5),
('cartoon', 'Мультфильмы', 'Cartoons', 'Персонажи из мультфильмов', 'Cartoon characters', 6),
('food', 'Еда', 'Food', 'Стикеры с едой и напитками', 'Food and drinks stickers', 7),
('nature', 'Природа', 'Nature', 'Природа и пейзажи', 'Nature and landscapes', 8),
('people', 'Люди', 'People', 'Люди и знаменитости', 'People and celebrities', 9),
('holiday', 'Праздники', 'Holidays', 'Праздничные стикеры', 'Holiday stickers', 10),
('work', 'Работа', 'Work', 'Рабочие стикеры', 'Work-related stickers', 11),
('love', 'Любовь', 'Love', 'Романтические стикеры', 'Romantic stickers', 12),
('funny', 'Смешные', 'Funny', 'Юмористические стикеры', 'Humorous stickers', 13),
('sport', 'Спорт', 'Sports', 'Спортивные стикеры', 'Sports stickers', 14),
('music', 'Музыка', 'Music', 'Музыкальные стикеры', 'Music stickers', 15);

-- Комментарии к таблицам
COMMENT ON TABLE categories IS 'Справочник категорий для стикерсетов';
COMMENT ON TABLE sticker_set_categories IS 'Промежуточная таблица связи стикерсетов и категорий';

COMMENT ON COLUMN categories.key IS 'Уникальный ключ категории (латиница, нижний регистр)';
COMMENT ON COLUMN categories.name_ru IS 'Название категории на русском языке';
COMMENT ON COLUMN categories.name_en IS 'Название категории на английском языке';
COMMENT ON COLUMN categories.display_order IS 'Порядок отображения категорий';
COMMENT ON COLUMN categories.is_active IS 'Флаг активности категории';


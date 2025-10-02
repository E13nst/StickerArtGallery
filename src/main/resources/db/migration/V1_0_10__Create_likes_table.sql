-- Создание таблицы для хранения лайков стикерсетов
CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sticker_set_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Один лайк от пользователя на стикерсет
    CONSTRAINT unique_user_sticker_like UNIQUE(user_id, sticker_set_id),
    
    -- Внешний ключ на таблицу стикерсетов
    CONSTRAINT fk_likes_sticker_set 
        FOREIGN KEY (sticker_set_id) 
        REFERENCES stickersets(id) 
        ON DELETE CASCADE
);

-- Индексы для производительности
CREATE INDEX idx_likes_user_id ON likes(user_id);
CREATE INDEX idx_likes_sticker_set_id ON likes(sticker_set_id);
CREATE INDEX idx_likes_created_at ON likes(created_at);

-- Составной индекс для быстрого поиска лайков пользователя по стикерсету
CREATE INDEX idx_likes_user_sticker_set ON likes(user_id, sticker_set_id);

-- Комментарии к таблице и полям
COMMENT ON TABLE likes IS 'Таблица для хранения лайков пользователей на стикерсеты';
COMMENT ON COLUMN likes.id IS 'Уникальный идентификатор лайка';
COMMENT ON COLUMN likes.user_id IS 'ID пользователя, который поставил лайк';
COMMENT ON COLUMN likes.sticker_set_id IS 'ID стикерсета, который лайкнули';
COMMENT ON COLUMN likes.created_at IS 'Дата и время создания лайка';

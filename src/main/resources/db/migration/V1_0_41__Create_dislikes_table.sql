-- Создание таблицы для хранения дизлайков стикерсетов
CREATE TABLE dislikes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stickerset_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Один дизлайк от пользователя на стикерсет
    CONSTRAINT unique_user_stickerset_dislike UNIQUE(user_id, stickerset_id),
    
    -- Внешний ключ на таблицу стикерсетов
    CONSTRAINT fk_dislikes_stickerset 
        FOREIGN KEY (stickerset_id) 
        REFERENCES stickersets(id) 
        ON DELETE CASCADE
);

-- Индексы для производительности
CREATE INDEX idx_dislikes_user_id ON dislikes(user_id);
CREATE INDEX idx_dislikes_stickerset_id ON dislikes(stickerset_id);
CREATE INDEX idx_dislikes_created_at ON dislikes(created_at);

-- Составной индекс для быстрого поиска дизлайков пользователя по стикерсету
CREATE INDEX idx_dislikes_user_stickerset ON dislikes(user_id, stickerset_id);

-- Комментарии к таблице и полям
COMMENT ON TABLE dislikes IS 'Таблица для хранения дизлайков пользователей на стикерсеты';
COMMENT ON COLUMN dislikes.id IS 'Уникальный идентификатор дизлайка';
COMMENT ON COLUMN dislikes.user_id IS 'ID пользователя, который поставил дизлайк';
COMMENT ON COLUMN dislikes.stickerset_id IS 'ID стикерсета, который дизлайкнули';
COMMENT ON COLUMN dislikes.created_at IS 'Дата и время создания дизлайка';

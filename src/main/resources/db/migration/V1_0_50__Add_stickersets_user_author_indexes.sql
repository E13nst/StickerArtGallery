-- Добавление индексов на stickersets.user_id и stickersets.author_id для оптимизации
-- агрегационных запросов в админ-панели (подсчет стикерсетов по владельцам и авторам)

-- Индекс на user_id для быстрой агрегации по владельцам
CREATE INDEX IF NOT EXISTS idx_stickersets_user_id 
ON stickersets(user_id);

COMMENT ON INDEX idx_stickersets_user_id IS 
'Индекс для оптимизации подсчета стикерсетов по владельцам (user_id)';

-- Индекс на author_id для быстрой агрегации по авторам
CREATE INDEX IF NOT EXISTS idx_stickersets_author_id 
ON stickersets(author_id) 
WHERE author_id IS NOT NULL;

COMMENT ON INDEX idx_stickersets_author_id IS 
'Partial индекс для оптимизации подсчета стикерсетов по авторам (author_id IS NOT NULL)';

-- Комбинированный индекс для фильтрации ACTIVE стикерсетов по user_id
CREATE INDEX IF NOT EXISTS idx_stickersets_state_user_id 
ON stickersets(state, user_id);

COMMENT ON INDEX idx_stickersets_state_user_id IS 
'Комбинированный индекс для оптимизации подсчета активных стикерсетов по владельцам';

-- Комбинированный индекс для фильтрации ACTIVE стикерсетов по author_id
CREATE INDEX IF NOT EXISTS idx_stickersets_state_author_id 
ON stickersets(state, author_id) 
WHERE author_id IS NOT NULL;

COMMENT ON INDEX idx_stickersets_state_author_id IS 
'Partial комбинированный индекс для оптимизации подсчета активных стикерсетов по авторам';

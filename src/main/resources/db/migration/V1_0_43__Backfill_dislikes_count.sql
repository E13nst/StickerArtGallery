-- Одноразовый бекфилл dislikes_count на основе таблицы dislikes
UPDATE stickersets s
SET dislikes_count = COALESCE(
  (
    SELECT COUNT(*)
    FROM dislikes d
    WHERE d.stickerset_id = s.id
  ), 0
);

-- Одноразовый бекфилл likes_count на основе таблицы likes
UPDATE stickersets s
SET likes_count = COALESCE(
  (
    SELECT COUNT(*)
    FROM likes l
    WHERE l.stickerset_id = s.id
  ), 0
);



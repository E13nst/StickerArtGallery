-- Переименование сущностей ленты: meme_* → style_feed_*
-- Таблицы: style_feed_items, style_feed_item_likes, style_feed_item_dislikes
-- user_swipes: style_feed_item_like_id / style_feed_item_dislike_id

ALTER TABLE user_swipes DROP CONSTRAINT IF EXISTS chk_user_swipes_one_ref;

-- Родительская таблица
ALTER TABLE meme_candidates RENAME TO style_feed_items;
ALTER TRIGGER trg_meme_candidates_set_updated_at ON style_feed_items
    RENAME TO trg_style_feed_items_set_updated_at;

ALTER INDEX idx_meme_candidates_visibility RENAME TO idx_style_feed_items_visibility;
ALTER INDEX idx_meme_candidates_task_id RENAME TO idx_style_feed_items_task_id;
ALTER INDEX idx_meme_candidates_preset_owner RENAME TO idx_style_feed_items_preset_owner;
ALTER INDEX idx_meme_candidates_cached_image RENAME TO idx_style_feed_items_cached_image;
ALTER INDEX uniq_meme_candidates_style_preset RENAME TO uniq_style_feed_items_style_preset;

-- Дочерние таблицы
ALTER TABLE meme_candidate_likes RENAME TO style_feed_item_likes;
ALTER TABLE meme_candidate_dislikes RENAME TO style_feed_item_dislikes;

ALTER INDEX idx_meme_candidate_likes_user RENAME TO idx_style_feed_item_likes_user;
ALTER INDEX idx_meme_candidate_likes_candidate RENAME TO idx_style_feed_item_likes_item;
ALTER INDEX idx_meme_candidate_dislikes_user RENAME TO idx_style_feed_item_dislikes_user;
ALTER INDEX idx_meme_candidate_dislikes_candidate RENAME TO idx_style_feed_item_dislikes_item;

ALTER TABLE style_feed_item_likes RENAME COLUMN meme_candidate_id TO style_feed_item_id;
ALTER TABLE style_feed_item_dislikes RENAME COLUMN meme_candidate_id TO style_feed_item_id;

ALTER TABLE style_feed_item_likes RENAME CONSTRAINT unique_user_meme_candidate_like TO unique_user_style_feed_item_like;
ALTER TABLE style_feed_item_dislikes RENAME CONSTRAINT unique_user_meme_candidate_dislike TO unique_user_style_feed_item_dislike;

-- user_swipes
ALTER INDEX idx_user_swipes_meme_like_id RENAME TO idx_user_swipes_style_feed_item_like_id;
ALTER INDEX idx_user_swipes_meme_dislike_id RENAME TO idx_user_swipes_style_feed_item_dislike_id;

ALTER TABLE user_swipes RENAME COLUMN meme_candidate_like_id TO style_feed_item_like_id;
ALTER TABLE user_swipes RENAME COLUMN meme_candidate_dislike_id TO style_feed_item_dislike_id;

ALTER TABLE user_swipes ADD CONSTRAINT chk_user_swipes_one_ref CHECK (
    (like_id IS NOT NULL
        AND dislike_id IS NULL
        AND style_feed_item_like_id IS NULL
        AND style_feed_item_dislike_id IS NULL
        AND action_type = 'LIKE') OR
    (dislike_id IS NOT NULL
        AND like_id IS NULL
        AND style_feed_item_like_id IS NULL
        AND style_feed_item_dislike_id IS NULL
        AND action_type = 'DISLIKE') OR
    (style_feed_item_like_id IS NOT NULL
        AND style_feed_item_dislike_id IS NULL
        AND like_id IS NULL
        AND dislike_id IS NULL
        AND action_type = 'LIKE') OR
    (style_feed_item_dislike_id IS NOT NULL
        AND style_feed_item_like_id IS NULL
        AND like_id IS NULL
        AND dislike_id IS NULL
        AND action_type = 'DISLIKE')
);

COMMENT ON COLUMN user_swipes.style_feed_item_like_id IS 'FK на style_feed_item_likes(id) — аналог like_id для ленты style feed';
COMMENT ON COLUMN user_swipes.style_feed_item_dislike_id IS 'FK на style_feed_item_dislikes(id) — аналог dislike_id для ленты style feed';

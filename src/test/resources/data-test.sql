-- Тестовые данные для автотестов

-- Очищаем таблицы
DELETE FROM stickersets;
DELETE FROM users;
DELETE FROM bot;

-- Вставляем тестового бота StickerGallery
INSERT INTO bot (name, username, token, type, is_active, created_at, updated_at) 
VALUES ('StickerGallery', 'StickerGalleryBot', 'test_token_for_sticker_gallery_bot', 'sticker', true, NOW(), NOW());

-- Вставляем тестового пользователя
INSERT INTO users (telegram_id, username, first_name, last_name, role, art_balance, created_at, updated_at) 
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', 0, NOW(), NOW());

-- Вставляем тестовые стикерсеты
INSERT INTO stickersets (user_id, title, name, created_at) 
VALUES 
(141614461, 'Тестовый набор 1', 'test_pack_1_by_StickerGalleryBot', NOW()),
(141614461, 'Тестовый набор 2', 'test_pack_2_by_StickerGalleryBot', NOW());

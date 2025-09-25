-- Обновление miniapp для бота StickerGallery
-- Дата: 2025-01-22

-- Проверяем, существует ли бот с name=StickerGallery
SELECT id, name, username, type, is_active, miniapp 
FROM bot 
WHERE name = 'StickerGallery';

-- Обновляем поле miniapp для бота StickerGallery
UPDATE bot 
SET miniapp = 'https://dalek-e13nst.amvera.io/mini-app/index.html'
WHERE name = 'StickerGallery';

-- Проверяем результат
SELECT id, name, username, type, is_active, miniapp 
FROM bot 
WHERE name = 'StickerGallery';

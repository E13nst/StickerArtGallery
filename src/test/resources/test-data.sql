-- Test data
INSERT INTO users (id, telegram_id, first_name, last_name, username, language_code, is_premium, allows_write_to_pm) VALUES
(1, 141614461, 'Andrey', 'Mitroshin', 'E13nst', 'ru', true, true),
(2, 999999999, 'Test', 'User', 'testuser', 'en', false, true);

-- Test sticker sets
INSERT INTO sticker_sets (id, user_id, title, name, created_at) VALUES
(1, 1, 'Test Stickers', 'test_stickers', CURRENT_TIMESTAMP),
(2, 1, 'Another Test Set', 'another_test_set', CURRENT_TIMESTAMP);

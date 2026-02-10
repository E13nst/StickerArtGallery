-- Remove user_profile_id from art_transactions to enforce telegram_user_id as identity
ALTER TABLE art_transactions
    DROP CONSTRAINT IF EXISTS fk_art_transactions_user_profile;

DROP INDEX IF EXISTS idx_art_transactions_user_profile_created_at;

ALTER TABLE art_transactions
    DROP COLUMN IF EXISTS user_profile_id;

-- Текст, который автор вводил в миниаппе при генерации перед публикацией стиля (для модерации)

ALTER TABLE style_presets
    ADD COLUMN IF NOT EXISTS submitted_user_prompt TEXT;

COMMENT ON COLUMN style_presets.submitted_user_prompt IS
    'Промпт из миниаппа на момент публикации (originalPrompt задачи генерации или fallback); только для аудита модератора';

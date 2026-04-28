CREATE UNIQUE INDEX uniq_meme_candidates_style_preset
    ON meme_candidates (style_preset_id)
    WHERE style_preset_id IS NOT NULL;

-- Публикация пресета из завершённой задачи генерации v2 (без черновика style_presets)

CREATE TABLE preset_publications_from_generation_tasks (
    id                  BIGSERIAL    PRIMARY KEY,
    generation_task_id  VARCHAR(255) NOT NULL,
    preset_id           BIGINT       NOT NULL REFERENCES style_presets(id) ON DELETE CASCADE,
    idempotency_key     VARCHAR(128) NOT NULL,
    charged_at          TIMESTAMPTZ,
    consent_at          TIMESTAMPTZ,
    display_name        VARCHAR(100),
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uniq_preset_pub_from_tasks_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_preset_pub_from_tasks_status CHECK (status IN ('PENDING', 'CHARGED', 'FAILED'))
);

CREATE UNIQUE INDEX ux_preset_pub_from_tasks_task_charged
    ON preset_publications_from_generation_tasks (generation_task_id)
    WHERE status = 'CHARGED';

CREATE INDEX idx_preset_pub_from_tasks_preset ON preset_publications_from_generation_tasks(preset_id);
CREATE INDEX idx_preset_pub_from_tasks_task ON preset_publications_from_generation_tasks(generation_task_id);

COMMENT ON TABLE preset_publications_from_generation_tasks IS
    'Идемпотентная публикация пользовательского пресета из COMPLETED задачи blueprint v2';

-- Миграция: правила ART для ручного начисления/списания администратором
-- Версия: 1.0.54
-- Описание: Добавление правил ADMIN_MANUAL_CREDIT и ADMIN_MANUAL_DEBIT (amount=0, переопределяется в запросе)

INSERT INTO art_rules (code, direction, amount, is_enabled, description)
VALUES
    ('ADMIN_MANUAL_CREDIT', 'CREDIT', 0, TRUE, 'Ручное начисление ART администратором (amount переопределяется в запросе)'),
    ('ADMIN_MANUAL_DEBIT', 'DEBIT', 0, TRUE, 'Ручное списание ART администратором (amount переопределяется в запросе)')
ON CONFLICT (code) DO NOTHING;

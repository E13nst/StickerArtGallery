# Telegram Chat + Inline Migration (Wave1)

Этот документ фиксирует запуск inbound Telegram updates в Java и полный cutover ownership после wave1.

## Что реализовано в wave1

- `POST /api/telegram/updates` — единая входная точка Telegram updates.
- Роутинг update-типов:
  - `message` (команды, входящие стикеры, `successful_payment`);
  - `callback_query` (`add_to_gallery`, support topics, `gen:/regen:`);
  - `inline_query` (поиск по галерее, fallback по `file_id`, generation placeholder);
  - `pre_checkout_query`;
  - `web_app_query` (минимальная `answerWebAppQuery` совместимость для `file_id:*`).
- Anti-duplicate защита по `update_id` (`TelegramUpdateDedupService`).
- Sticker flow:
  - входящий стикер -> проверка наличия набора в БД;
  - callback `add_to_gallery:<set>:<messageId>`;
  - реакция `✅`/`👍`, создание public набора и начисление ART через существующий `RULE_UPLOAD_STICKERSET`.
- Support bridge:
  - `/support`, `support_topic:*`, `exit_support`;
  - forum topics в support-чате (если включено);
  - двусторонняя пересылка (text/photo/document/voice/video/sticker).
- Payment updates:
  - `answerPreCheckoutQuery`;
  - обработка `successful_payment` через `StarsPaymentService`.
- Inline:
  - поиск по галерее;
  - direct `file_id` режим (`file_id:<...>` или raw file_id);
  - inline generation callbacks `gen:` / `regen:` с async pipeline и выдачей результата в чат.

## Новые env переменные

```bash
TELEGRAM_WEBHOOK_SECRET_TOKEN=...   # опционально, если хотите строго валидировать X-Telegram-Bot-Api-Secret-Token
TELEGRAM_SUPPORT_ENABLED=true
TELEGRAM_SUPPORT_CHAT_ID=-100...
TELEGRAM_SUPPORT_USE_TOPICS=true
```

Уже существующие (из предыдущей миграции) также обязательны к проверке:

```bash
TELEGRAM_NATIVE_PAYMENT_ENABLED=true
TELEGRAM_NATIVE_MESSAGING_ENABLED=true
TELEGRAM_WEBHOOK_OWNER=java
WEBHOOK_HMAC_SECRET=...
WEBHOOK_HMAC_ENFORCED=true
```

## Smoke-план перед cutover

1. **Sticker flow**
   - Отправить стикер из неизвестного набора в ЛС боту.
   - Нажать `Добавить в галерею`.
   - Проверить: success-сообщение, reaction `👍`, набор появился в галерее.
2. **Support**
   - Выполнить `/support` -> выбрать тему.
   - Отправить сообщение пользователем, убедиться что оно в support-чате (в topic, если включено).
   - Ответить оператором `reply` на forwarded сообщение, убедиться что ответ пришел пользователю.
3. **Payments**
   - Оплатить тестовый invoice.
   - Проверить, что `pre_checkout_query` подтверждается и `successful_payment` обрабатывается (ART начислен).
4. **Inline**
   - `@bot query` — результаты поиска.
   - `@bot file_id:...` — direct cached sticker.
   - `@bot gen: ...` -> кнопка запуска -> результат/ошибка в чате.

## Cutover runbook (single owner)

1. Убедиться, что wave1 smoke завершен успешно.
2. Удалить старый webhook:
   - `DELETE /api/internal/telegram/ownership/webhook`
3. Установить webhook на Java:
   - `POST /api/internal/telegram/ownership/webhook`
   - `webhookUrl = https://<your-domain>/api/telegram/updates`
   - `secretToken = $TELEGRAM_WEBHOOK_SECRET_TOKEN` (если используете)
4. Переключить owner:
   - `TELEGRAM_WEBHOOK_OWNER=java`
5. Наблюдать 24-72 часа:
   - ошибки в `/api/telegram/updates`;
   - дубли/потеря updates;
   - задержки payment/support/inline.

## Rollback

1. `deleteWebhook` на Java.
2. `setWebhook` обратно на StickerBot endpoint.
3. `TELEGRAM_WEBHOOK_OWNER=stickerbot`.
4. Проверить smoke (минимум: `/start`, платеж, support).

## Decommission StickerBot

После стабильного окна наблюдения:

- выключить deployment StickerBot;
- удалить связанные env:
  - `STICKERBOT_API_URL`
  - `STICKERBOT_SERVICE_TOKEN`
- обновить операционные runbook'и под single-owner Java inbound.

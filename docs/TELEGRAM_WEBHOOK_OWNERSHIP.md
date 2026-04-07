# Telegram Webhook Ownership: инвариант и runbook

## Ключевой инвариант

> **Для одного `BOT_TOKEN` в каждый момент времени есть строго один владелец входящих Telegram updates.**

- Нельзя одновременно иметь две активные `setWebhook` подписки на один токен — Telegram отклонит вторую.
- Нельзя одновременно запустить polling в двух процессах с одним токеном — возникнет гонка получения updates.
- **Переключение ownership (cutover) — атомарная операция**: сначала отключаем старого владельца, потом подключаем нового.

---

## Текущее состояние (до cutover)

```
Telegram Bot API (single BOT_TOKEN)
         │
         ▼ webhook / polling
   StickerBot (Python FastAPI)     ← текущий владелец updates
         │
         │  POST /api/internal/webhooks/stars-payment
         │  X-Service-Token: <token>
         ▼
   Java Backend (Spring Boot)      ← бизнес-логика, payments, gallery
```

Конфигурация `StickerBot`:
- Переменная `BOT_WEBHOOK_URL` → `https://stickerartgallery-e13nst.amvera.io/webhook`
- Сервис слушает обновления Telegram и нотифицирует Java через webhook callback

Конфигурация `Java Backend`:
- `app.telegram.bot-token` — используется для прямых API-вызовов (getStickerSet, createInvoiceLink, sendMessage) **без владения updates**
- `app.stickerbot.api-url` / `app.stickerbot.service-token` — для вызова StickerBot API

---

## Целевое состояние (после cutover)

```
Telegram Bot API (single BOT_TOKEN)
         │
         ▼ webhook (only Java registered)
   Java Backend (Spring Boot)      ← единственный владелец updates
         │
         ├─ payment updates (pre_checkout, successful_payment)
         ├─ sendMessage (прямо через Bot API)
         └─ createInvoiceLink (прямо через Bot API)

   StickerBot                      ← выведен из эксплуатации
```

---

## Флаги управления миграцией

В `application.yaml` / env-переменных:

| Переменная | По умолчанию | Описание |
|---|---|---|
| `TELEGRAM_NATIVE_PAYMENT_ENABLED` | `false` | Java создаёт invoice напрямую через Bot API (минует StickerBot) |
| `TELEGRAM_NATIVE_MESSAGING_ENABLED` | `false` | Java отправляет сообщения напрямую через Bot API |
| `TELEGRAM_WEBHOOK_OWNER` | `stickerbot` | Текущий владелец updates: `stickerbot` или `java` |
| `WEBHOOK_HMAC_SECRET` | `""` | Секрет для HMAC-SHA256 проверки входящих webhook callbacks |
| `WEBHOOK_HMAC_ENFORCED` | `false` | `true` = reject webhooks без валидной подписи |

---

## Runbook: переключение ownership на Java

### Предусловия (все должны быть выполнены)

- [ ] `TELEGRAM_NATIVE_PAYMENT_ENABLED=true` задеплоено и работает без ошибок
- [ ] `TELEGRAM_NATIVE_MESSAGING_ENABLED=true` задеплоено и работает без ошибок
- [ ] Shadow-сравнение платежей: 0 критичных расхождений за ≥24 часа
- [ ] `WEBHOOK_HMAC_SECRET` настроен; HMAC проверка проходит positive и negative тесты
- [ ] Runbook rollback проверен на staging
- [ ] Мониторинг/алерты настроены для payment flow Java

### Шаги cutover (последовательно, в одном деплой-окне)

**Шаг 1 — Остановить прием updates в StickerBot**

```bash
# Вариант A: через control API StickerBot
curl -X POST https://stixly-e13nst.amvera.io/api/control/stop \
  -H "Authorization: Bearer <STICKERBOT_CONTROL_TOKEN>"

# Вариант B: через Telegram API — снять webhook
curl "https://api.telegram.org/bot<BOT_TOKEN>/deleteWebhook"
```

Ожидаем: StickerBot перестаёт получать updates, Telegram подтверждает `{"ok":true}`.

**Шаг 2 — Зарегистрировать webhook на Java Backend**

```bash
curl -X POST "https://api.telegram.org/bot<BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://stickerartgallery-e13nst.amvera.io/api/telegram/updates",
    "secret_token": "<TELEGRAM_WEBHOOK_SECRET>",
    "allowed_updates": ["message","pre_checkout_query","successful_payment","callback_query","inline_query","chosen_inline_result"]
  }'
```

Ожидаем: Telegram отвечает `{"ok":true,"result":true}`.

**Шаг 3 — Установить флаг владельца в Java**

```bash
# В переменных окружения деплоя:
TELEGRAM_WEBHOOK_OWNER=java
```

Перезапустить Java Backend (или использовать hot-reload если настроен).

**Шаг 4 — Smoke-тесты (немедленно после cutover)**

```bash
# Test 1: Получить webhook info
curl "https://api.telegram.org/bot<BOT_TOKEN>/getWebhookInfo"
# Ожидаем: url содержит stickerartgallery, pending_update_count близко к 0

# Test 2: Создать invoice через Java API
curl -X POST https://stickerartgallery-e13nst.amvera.io/api/stars/create-invoice \
  -H "X-Telegram-Init-Data: ..." \
  -H "Content-Type: application/json" \
  -d '{"packageCode":"STARTER"}'
# Ожидаем: invoiceUrl начинается с https://t.me/$

# Test 3: Проверить логи Java на входящие payment updates
# Ожидаем: pre_checkout_query и successful_payment обрабатываются без ошибок
```

### Rollback (если что-то пошло не так)

**Максимальное время rollback: 5 минут.**

```bash
# Шаг R1 — Снять webhook Java
curl "https://api.telegram.org/bot<BOT_TOKEN>/deleteWebhook"

# Шаг R2 — Вернуть webhook StickerBot
curl -X POST "https://api.telegram.org/bot<BOT_TOKEN>/setWebhook" \
  -d "url=https://stixly-e13nst.amvera.io/webhook"

# Шаг R3 — Вернуть флаги
TELEGRAM_WEBHOOK_OWNER=stickerbot
TELEGRAM_NATIVE_PAYMENT_ENABLED=false
TELEGRAM_NATIVE_MESSAGING_ENABLED=false
```

**После rollback:** найти причину отказа, зафиксировать в инциденте, исправить до следующего cutover.

---

## Критерии готовности к decommission StickerBot

После успешного cutover и стабильной работы Java как владельца updates в течение ≥72 часов:

- [ ] Убрать вызовы к StickerBot API из Java (`/api/payments/create-invoice`, `/api/messages/send`)
- [ ] Убрать `STICKERBOT_API_URL`, `STICKERBOT_SERVICE_TOKEN` из env
- [ ] Остановить StickerBot деплой
- [ ] Обновить архитектурную документацию

---

## Контакты и ответственные

- Владелец миграции: @team
- Rollback decision authority: on-call engineer
- Мониторинг payment flow: `/api/internal/webhooks/stars-payment` логи + алерты по 5xx

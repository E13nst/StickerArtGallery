# Telegram Stars Payment Integration

## Обзор

Интеграция Telegram Stars для покупки ART-баллов. Поддерживает два режима работы:

- **Legacy (по умолчанию)**: Python StickerBot получает webhook от Telegram и пересылает в Java.
- **Native (целевой)**: Java Backend напрямую работает с Telegram Bot API, StickerBot не задействован.

Подробнее о миграции: [TELEGRAM_WEBHOOK_OWNERSHIP.md](./TELEGRAM_WEBHOOK_OWNERSHIP.md)

## Архитектура

### Legacy (TELEGRAM_NATIVE_PAYMENT_ENABLED=false)

```
Mini App → Java REST API → StickerBot API (create-invoice)
                                    ↓
                            Telegram Payment
                                    ↓
                         StickerBot (webhook owner)
                                    ↓
                   POST /api/internal/webhooks/stars-payment
                   X-Service-Token + X-Webhook-Signature
                                    ↓
                            StarsPaymentService
                                    ↓
                            ArtRewardService (начисление ART)
```

### Native (TELEGRAM_NATIVE_PAYMENT_ENABLED=true, после cutover)

```
Mini App → Java REST API → Telegram Bot API (createInvoiceLink)
                                    ↓
                            Telegram Payment
                                    ↓
                   Java Backend (webhook owner, TELEGRAM_WEBHOOK_OWNER=java)
                                    ↓
                            StarsPaymentService
                                    ↓
                            ArtRewardService (начисление ART)
```

## Python Bot Integration

### Установка handlers

В главном файле Python бота добавьте:

```python
from payment_handlers import pre_checkout_query_handler, successful_payment_handler
from telegram.ext import PreCheckoutQueryHandler, MessageHandler, filters

# Регистрация handlers
application.add_handler(PreCheckoutQueryHandler(pre_checkout_query_handler))
application.add_handler(MessageHandler(filters.SUCCESSFUL_PAYMENT, successful_payment_handler))
```

### Переменные окружения

Добавьте в `.env` Python бота:

```bash
JAVA_API_URL=https://your-java-api-url.com
SERVICE_TOKEN=your-service-token-from-java-config
```

### Использование

1. Пользователь выбирает пакет в Mini App
2. Mini App вызывает `POST /api/stars/create-invoice` с `packageCode`
3. Java API создает запись в `stars_invoice_intents`, вызывает внешний StickerBot API (`/api/payments/create-invoice`) и возвращает `invoiceUrl`
4. Mini App открывает invoice URL
5. Пользователь оплачивает Stars
6. Telegram отправляет `pre_checkout_query` → Python бот → Java API валидирует
7. Telegram отправляет `successful_payment` → Python бот → Java API обрабатывает и начисляет ART

## Java API Endpoints

### User API

- `GET /api/stars/packages` - список активных пакетов (публичный)
- `POST /api/stars/create-invoice` - создание invoice (требует авторизации)
- `GET /api/stars/purchases` - история покупок пользователя
- `GET /api/stars/purchases/recent` - последняя покупка пользователя

### Admin API

- `GET /api/admin/stars/packages` - все пакеты
- `POST /api/admin/stars/packages` - создать пакет
- `PUT /api/admin/stars/packages/{id}` - обновить пакет
- `PATCH /api/admin/stars/packages/{id}/toggle` - включить/выключить пакет
- `GET /api/admin/stars/purchases` - все покупки

### Internal API (для Python бота / Java-обработки)

- `POST /api/internal/webhooks/stars-payment` - webhook обработки успешного платежа

Internal endpoint требует:
- `X-Service-Token` — межсервисный токен (всегда обязателен)
- `X-Webhook-Signature` — HMAC-SHA256 подпись тела (опционально при `WEBHOOK_HMAC_ENFORCED=false`, обязательно при `true`)

### Admin API управления webhook ownership

- `GET /api/internal/telegram/ownership/status` — текущий webhook info + bot status
- `DELETE /api/internal/telegram/ownership/webhook` — удалить webhook (шаг 1 cutover)
- `POST /api/internal/telegram/ownership/webhook` — зарегистрировать webhook на Java URL (шаг 2 cutover)

Эти endpoints защищены `X-Service-Token`.

## База данных

Миграция `V1_0_46__Create_stars_packages_and_purchases.sql` создает:

- `stars_packages` - тарифные пакеты
- `stars_invoice_intents` - намерения покупки
- `stars_purchases` - история покупок
- `stars_products` - универсальные продукты (для будущего)

## Безопасность

1. **Service Token** (`X-Service-Token`) — все internal endpoints защищены токеном `SERVICE_API_TOKEN`
2. **HMAC подпись** (`X-Webhook-Signature`) — HMAC-SHA256 тела запроса с ключом `WEBHOOK_HMAC_SECRET`
   - Вычисляется как `HMAC-SHA256(secret, canonical_json)`, где canonical JSON — ключи по алфавиту, без пробелов
   - Верификатор: `WebhookHmacVerifier.java`
3. **Идемпотентность** — `telegram_charge_id` как уникальный ключ защищает от двойного начисления
4. **Транзакционность** — все операции начисления ART в `@Transactional`
5. **Валидация суммы** — стоимость пакета сверяется с полученной `amount_stars`
6. **Shadow-сверка** — `PaymentShadowValidationService` логирует расхождения ([SHADOW_MISMATCH]) без влияния на ответ

### Переменные окружения для security

| Переменная | Обязательность | Описание |
|---|---|---|
| `SERVICE_API_TOKEN` | Обязательна | Токен для X-Service-Token |
| `WEBHOOK_HMAC_SECRET` | Рекомендована | Секрет для X-Webhook-Signature HMAC проверки |
| `WEBHOOK_HMAC_ENFORCED` | По умолчанию `false` | `true` = отклонять запросы без HMAC |

## Расширяемость

Таблица `stars_products` позволяет в будущем добавить:
- 🌟 Highlight стикерсета в галерее
- 💎 Premium подписка
- 🎁 Разовые функции

Для этого потребуется только добавить обработчики в `StarsPaymentService` под разные `product_type`.

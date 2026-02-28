# Telegram Stars Payment Integration

## Обзор

Интеграция Telegram Stars для покупки ART-баллов. Python бот получает webhook события от Telegram и передает их в Java API для обработки.

## Архитектура

```
Mini App → Java REST API → StickerBot API (create-invoice)
                                    ↓
                            Telegram Payment
                                    ↓
                            Python Bot (webhook)
                                    ↓
                            Java Internal API
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

### Internal API (для Python бота)

- `POST /api/internal/webhooks/stars-payment` - webhook обработки успешного платежа

Internal endpoint требует заголовок `X-Service-Token`.

## База данных

Миграция `V1_0_46__Create_stars_packages_and_purchases.sql` создает:

- `stars_packages` - тарифные пакеты
- `stars_invoice_intents` - намерения покупки
- `stars_purchases` - история покупок
- `stars_products` - универсальные продукты (для будущего)

## Безопасность

1. **Service Token** - все internal endpoints защищены токеном
2. **Идемпотентность** - используется `telegram_payment_id` и `telegram_charge_id` как уникальные ключи
3. **Транзакционность** - все операции начисления ART в `@Transactional`
4. **Валидация** - проверка суммы и пакета перед оплатой

## Расширяемость

Таблица `stars_products` позволяет в будущем добавить:
- 🌟 Highlight стикерсета в галерее
- 💎 Premium подписка
- 🎁 Разовые функции

Для этого потребуется только добавить обработчики в `StarsPaymentService` под разные `product_type`.

# Telegram Stars Payment Integration - Упрощенная версия

## 🎯 Изменения в архитектуре

**Упрощена авторизация**: убрана HMAC проверка, используется только Service Token.

### Было (двойная авторизация):
```
StickerBot → Java Backend
├─ X-Service-Token (для всех internal endpoints)
└─ X-Webhook-Signature (HMAC, только для webhook)
```

### Стало (единая авторизация):
```
StickerBot → Java Backend
└─ X-Service-Token (для всех /api/internal/** endpoints)
```

---

## 🔐 Авторизация

**Все `/api/internal/**` endpoints** используют **только `X-Service-Token`**:

### Авторизация в запросах:
```http
POST /api/internal/webhooks/stars-payment
Content-Type: application/json
X-Service-Token: your_service_token_here

{
  "event": "telegram_stars_payment_succeeded",
  "user_id": 123456789,
  "amount_stars": 50,
  "currency": "XTR",
  "telegram_charge_id": "unique_charge_id",
  "invoice_payload": "{\"package_id\": 1}",
  "timestamp": 1234567890
}
```

**Response:**
```json
{
  "success": true,
  "purchaseId": 123,
  "artCredited": 100,
  "errorMessage": null
}
```

---

## 🔧 Конфигурация

### Backend (.env.app)
```bash
STICKERBOT_SERVICE_TOKEN=your_service_token_here
```

### application.yaml
```yaml
app:
  internal:
    service-tokens:
      sticker-bot: ${STICKERBOT_SERVICE_TOKEN:}
```

---

## 📦 Защита от дубликатов

**Idempotency** через `telegram_charge_id`:
- Повторные запросы с тем же `telegram_charge_id` возвращают существующий `purchaseId`
- Не создают дубликаты в БД
- Не начисляют ART повторно

---

## ✅ Что было удалено

1. ❌ `WebhookSignatureValidator.java`
2. ❌ `WebhookSignatureValidatorTest.java`
3. ❌ `X-Webhook-Signature` header
4. ❌ `app.telegram.webhook.secret` из конфигурации
5. ❌ `BACKEND_WEBHOOK_SECRET` из .env.app
6. ❌ HMAC проверка в контроллере

---

## ✅ Что осталось

1. ✅ `ServiceTokenAuthenticationFilter` - проверяет `X-Service-Token`
2. ✅ `@PreAuthorize("hasRole('INTERNAL')")` - авторизация на уровне Spring Security
3. ✅ Idempotency проверка по `telegram_charge_id`
4. ✅ Валидация package_id и amount_stars
5. ✅ HTTPS защита (на уровне транспорта)

---

## 🧪 Тестирование

### Локальное тестирование
```bash
# Запуск тестов (без HMAC подписи)
./scripts/test-stars-payment.sh http://localhost:8080 your_service_token
```

### Integration тесты
```bash
./gradlew test --tests TelegramWebhookIntegrationTest
```

**Все тесты обновлены**: проверяют только `X-Service-Token`, HMAC проверки удалены.

---

## 🚀 Преимущества упрощения

1. **Единообразие**: один способ авторизации для всех internal endpoints
2. **Простота**: StickerBot использует один header для всех запросов
3. **Меньше кода**: меньше классов, меньше конфигурации
4. **Достаточная безопасность**:
   - HTTPS защищает от перехвата
   - Service Token аутентифицирует сервис
   - Idempotency защищает от дублей

---

## 📚 Обновленная документация

- **[README.md](../README.md)** - обновлен раздел Telegram Stars
- **[STARS_PAYMENT_SIMPLIFIED.md](STARS_PAYMENT_SIMPLIFIED.md)** - этот файл
- **[TelegramWebhookIntegrationTest.java](../src/test/java/com/example/sticker_art_gallery/controller/internal/TelegramWebhookIntegrationTest.java)** - обновлены все тесты

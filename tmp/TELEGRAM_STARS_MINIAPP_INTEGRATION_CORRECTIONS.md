# Исправления для TELEGRAM_STARS_MINIAPP_INTEGRATION.md

## Критические изменения после внедрения новой архитектуры

### 1. URL Endpoint

**❌ СТАРОЕ (неправильно):**
```
backend_webhook_url: "https://backend.example.com/api/payments/telegram"
```

**✅ НОВОЕ (правильно):**
```
backend_webhook_url: "https://backend.example.com/api/internal/stars/telegram-webhook"
```

**Где заменить:**
- Строка 91
- Строка 184
- Строка 317
- Строка 543

---

### 2. package_id формат

**❌ СТАРОЕ (неправильно) - строка/code:**
```json
"payload": "{\"package_id\": \"basic_10\"}"
```

**✅ НОВОЕ (правильно) - число/ID:**
```json
"payload": "{\"package_id\": 1}"
```

**Комментарий для добавления:**
```javascript
// package_id должен быть числом (ID из StarsPackageEntity), НЕ строкой (code)!
// Получить можно через GET /api/stars/packages
payload: JSON.stringify({ 
  package_id: 1,  // ✅ Число (например, из API: packages[0].id)
  timestamp: Date.now()
})
```

**Где заменить:**
- Строка 89
- Строка 180
- Строка 421
- Строка 457
- Строка 664

---

### 3. Headers для webhook

**Секция "Backend Webhook Integration" - Добавить:**

```
**Headers:**
Content-Type: application/json; charset=utf-8
X-Service-Token: <service_token>           # 👈 ОБЯЗАТЕЛЬНО!
X-Webhook-Signature: {hmac_sha256_hex}     # Если настроен BACKEND_WEBHOOK_SECRET
User-Agent: StickerBot-WebhookNotifier/1.0
```

**Описание X-Service-Token:**
```
**X-Service-Token** - токен для межсервисной аутентификации (защита internal API endpoints).
Должен совпадать с STICKERBOT_SERVICE_TOKEN на Java backend.
```

---

### 4. Обновленный Java пример

**Заменить секцию начиная со строки 735:**

```java
@PostMapping("/api/internal/stars/telegram-webhook")  // ✅ Новый endpoint
public ResponseEntity<?> handleWebhook(
    @RequestBody String requestBody,
    @RequestHeader(value = "X-Webhook-Signature", required = false) String signature
) {
    // 1. Проверка HMAC подписи
    String secret = System.getenv("BACKEND_WEBHOOK_SECRET");
    if (secret != null && !secret.isEmpty()) {
        if (signature == null || !verifyWebhookSignature(signature, requestBody, secret)) {
            return ResponseEntity.status(401).body("{\"error\":\"Invalid signature\"}");
        }
    }
    
    // 2. Парсим payload
    TelegramWebhookRequest request = objectMapper.readValue(requestBody, TelegramWebhookRequest.class);
    
    // 3. Обработка платежа
    if ("telegram_stars_payment_succeeded".equals(request.getEvent())) {
        ProcessPaymentResponse response = starsPaymentService.processWebhookPayment(request);
        
        if (response.getSuccess()) {
            // ✅ Платеж успешно обработан
            return ResponseEntity.ok(response);
        } else {
            // ❌ Ошибка обработки
            return ResponseEntity.ok(response); // 200 OK, но success=false
        }
    }
    
    return ResponseEntity.badRequest().body("{\"error\":\"Unknown event\"}");
}
```

**Формат ответа:**
```json
{
  "success": true,
  "purchaseId": 123,
  "artCredited": 100,
  "errorMessage": null
}
```

---

### 5. Обновленная секция "Конфигурация"

**Добавить в раздел "Backend (.env)" после строки 588:**

```bash
# Переменные для Java Backend (обязательно!)
BACKEND_WEBHOOK_SECRET=<64_hex_символа>        # Для HMAC подписи
STICKERBOT_SERVICE_TOKEN=<service_token>       # Для X-Service-Token

# Endpoint для webhook (внутренний API)
# Используется в frontend при создании invoice
BACKEND_WEBHOOK_URL=https://your-backend.com/api/internal/stars/telegram-webhook
```

**Генерация секретов:**
```bash
# BACKEND_WEBHOOK_SECRET (для HMAC) - 64 hex символа
python3 -c "import secrets; print(secrets.token_hex(32))"

# STICKERBOT_SERVICE_TOKEN (для X-Service-Token)
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

**⚠️ Важно:** Оба секрета должны совпадать между Python Service и Java Backend!

---

### 6. Обновить пример Frontend кода

**Строка 174-185 - обновить:**

```javascript
// Получаем список пакетов для получения ID
const packagesResponse = await fetch('https://your-backend.com/api/stars/packages');
const packages = await packagesResponse.json();
const selectedPackage = packages.find(p => p.code === 'BASIC'); // Находим по code

// Создаем invoice через Bot API
const response = await fetch('https://your-bot-api.com/api/payments/create-invoice', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Telegram-Init-Data': initData
  },
  body: JSON.stringify({
    user_id: userId,
    title: title,
    description: description,
    amount_stars: amountStars,
    payload: JSON.stringify({ 
      package_id: selectedPackage.id,  // ✅ Используем ID (число), не code!
      timestamp: Date.now()
    }),
    return_link: true,
    backend_webhook_url: 'https://your-backend.com/api/internal/stars/telegram-webhook'
  })
});
```

---

### 7. Добавить новую секцию "Troubleshooting"

**После строки 561 добавить:**

```markdown
### Проблема: "Пакет не найден: package_id"

**Причина:** В payload передан `package_code` (строка) вместо `package_id` (число)

**Решение:**
```javascript
// ❌ НЕПРАВИЛЬНО
payload: JSON.stringify({ package_id: "BASIC" })

// ✅ ПРАВИЛЬНО
const packages = await fetch('/api/stars/packages').then(r => r.json());
const packageId = packages.find(p => p.code === 'BASIC').id;
payload: JSON.stringify({ package_id: packageId })  // Число!
```

### Проблема: "Invalid signature" (401)

**Причина:** Не совпадают секреты BACKEND_WEBHOOK_SECRET между Python и Java

**Решение:**
1. Сгенерируйте новый секрет:
   ```bash
   python3 -c "import secrets; print(secrets.token_hex(32))"
   ```
2. Установите **ОДИНАКОВЫЙ** секрет в обоих `.env` файлах
3. Перезапустите оба сервиса

### Проблема: "Missing service token" (401)

**Причина:** Не передан X-Service-Token header или не совпадает с Java backend

**Решение:**
1. Убедитесь что Python сервис добавляет заголовок:
   ```python
   headers = {
       "X-Service-Token": SERVICE_TOKEN,
       "X-Webhook-Signature": hmac_signature,
       # ...
   }
   ```
2. Проверьте что токены совпадают:
   - Python: `SERVICE_TOKEN` в `.env`
   - Java: `STICKERBOT_SERVICE_TOKEN` в `.env.app`
```

---

## Чеклист изменений

- [ ] Заменить все `/api/payments/telegram` → `/api/internal/stars/telegram-webhook`
- [ ] Заменить примеры `package_id: "basic_10"` → `package_id: 1`
- [ ] Добавить описание X-Service-Token header
- [ ] Обновить Java пример кода
- [ ] Добавить формат ответа ProcessPaymentResponse
- [ ] Обновить секцию конфигурации (добавить переменные)
- [ ] Добавить примеры генерации секретов
- [ ] Обновить Frontend пример (получение ID из API)
- [ ] Добавить новые troubleshooting секции
- [ ] Добавить предупреждение о синхронизации секретов

---

**Дата:** 2026-02-03  
**Статус:** Готово к внедрению  
**Приоритет:** 🔴 Критический (без этих изменений интеграция не будет работать!)

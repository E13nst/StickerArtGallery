# Миграция Python сервиса для работы с новым Java Backend API

## Критические изменения

### 1. URL Endpoint

**Было:**
```python
backend_webhook_url = "https://backend.example.com/api/payments/telegram"
```

**Должно быть:**
```python
backend_webhook_url = "https://backend.example.com/api/internal/stars/telegram-webhook"
```

**Где изменить:**
- Файл: `src/services/webhook_notifier.py` (или аналогичный)
- Переменная окружения: `BACKEND_WEBHOOK_URL`

---

### 2. Headers - Добавить X-Service-Token

**Было:**
```python
headers = {
    "Content-Type": "application/json; charset=utf-8",
    "X-Webhook-Signature": hmac_signature,
    "User-Agent": "StickerBot-WebhookNotifier/1.0"
}
```

**Должно быть:**
```python
headers = {
    "Content-Type": "application/json; charset=utf-8",
    "X-Webhook-Signature": hmac_signature,
    "X-Service-Token": SERVICE_TOKEN,  # 👈 ДОБАВИТЬ!
    "User-Agent": "StickerBot-WebhookNotifier/1.0"
}
```

**Причина:** Java backend требует оба заголовка:
- `X-Service-Token` для ServiceTokenAuthenticationFilter
- `X-Webhook-Signature` для дополнительной проверки HMAC

---

### 3. Invoice Payload Format

**Критически важно:** `invoice_payload` должен содержать JSON с `package_id` как **число** (ID пакета из StarsPackageEntity).

**Правильный формат:**
```json
{
  "package_id": 1
}
```

**НЕ правильно:**
```json
{
  "package_id": "basic_10"  // ❌ НЕ code пакета!
}
```

#### Как получить package_id?

**Вариант 1:** Frontend передает package_id напрямую
```javascript
// Frontend Mini App
payload: JSON.stringify({ package_id: 1 })  // Число!
```

**Вариант 2:** Python API получает package_id из запроса
```python
# POST /api/payments/create-invoice
request_data = {
    "user_id": 141614461,
    "package_id": 1,  # ID пакета (число)
    "title": "Пакет генераций",
    "description": "Пакет на 10 генераций",
    "amount_stars": 100
}
```

**Вариант 3:** Маппинг code → id (если frontend передает code)
```python
# Если frontend передает package_code вместо package_id
PACKAGE_CODE_TO_ID = {
    "STARTER": 1,
    "BASIC": 2,
    "PREMIUM": 3,
    # ... и т.д.
}

package_code = request.get("package_code")
package_id = PACKAGE_CODE_TO_ID.get(package_code)
if not package_id:
    raise ValueError(f"Unknown package code: {package_code}")

invoice_payload = json.dumps({"package_id": package_id})
```

---

### 4. Переменные окружения

**Добавить/обновить в `.env`:**

```bash
# URL Java backend webhook endpoint
BACKEND_WEBHOOK_URL=https://your-backend.com/api/internal/stars/telegram-webhook

# Service Token для X-Service-Token header
SERVICE_TOKEN=<ваш_service_token>

# HMAC секрет для X-Webhook-Signature (64 hex символа)
BACKEND_WEBHOOK_SECRET=<64_hex_символа>
```

**Генерация секретов:**
```bash
# BACKEND_WEBHOOK_SECRET (для HMAC)
python3 -c "import secrets; print(secrets.token_hex(32))"

# SERVICE_TOKEN (для X-Service-Token) - должен совпадать с Java backend
# Получить из Java backend переменной окружения STICKERBOT_SERVICE_TOKEN
```

**Важно:** Оба секрета должны совпадать с Java backend!

---

### 5. Обработка ответов от Java Backend

**Java backend возвращает:**
```json
{
  "success": true,
  "purchaseId": 123,
  "artCredited": 100,
  "errorMessage": null
}
```

**Python должен:**

```python
async def send_webhook(webhook_data: dict) -> bool:
    """Отправляет webhook на Java backend"""
    
    # Генерация canonical JSON и HMAC подписи
    canonical_json = _canonical_json(webhook_data)
    hmac_signature = _generate_hmac_signature(canonical_json)
    
    headers = {
        "Content-Type": "application/json; charset=utf-8",
        "X-Webhook-Signature": hmac_signature,
        "X-Service-Token": SERVICE_TOKEN,
        "User-Agent": "StickerBot-WebhookNotifier/1.0"
    }
    
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                BACKEND_WEBHOOK_URL,
                content=canonical_json,  # Отправляем canonical JSON
                headers=headers
            )
            
            # Проверка успешности
            if response.status_code == 200:
                result = response.json()
                
                if result.get("success"):
                    logger.info(f"✅ Webhook успешно доставлен: purchaseId={result.get('purchaseId')}, artCredited={result.get('artCredited')}")
                    return True
                else:
                    error_msg = result.get("errorMessage", "Unknown error")
                    logger.error(f"❌ Backend вернул ошибку: {error_msg}")
                    return False  # Retry
            
            elif response.status_code == 409:
                # Дубликат платежа - считается успехом
                logger.info("♻️ Платеж уже обработан (идемпотентность)")
                return True
            
            elif response.status_code == 401:
                # Невалидная подпись - НЕ retry
                logger.error("❌ Невалидная HMAC подпись или service token")
                return False
            
            else:
                # Другие ошибки - retry
                logger.error(f"❌ Backend вернул ошибку: {response.status_code}")
                return False  # Retry
                
    except Exception as e:
        logger.error(f"❌ Ошибка отправки webhook: {e}")
        return False  # Retry
```

---

### 6. Полный пример webhook payload

**Формат данных для отправки на Java backend:**

```python
webhook_data = {
    "event": "telegram_stars_payment_succeeded",
    "user_id": 141614461,
    "amount_stars": 100,
    "currency": "XTR",
    "telegram_charge_id": "1234567890",
    "invoice_payload": json.dumps({"package_id": 1}),  # JSON строка!
    "timestamp": int(time.time())
}
```

**После canonical JSON и HMAC:**
```
POST /api/internal/stars/telegram-webhook HTTP/1.1
Host: your-backend.com
Content-Type: application/json; charset=utf-8
X-Webhook-Signature: a1b2c3d4e5f6789... (64 hex символа)
X-Service-Token: your_service_token
User-Agent: StickerBot-WebhookNotifier/1.0

{"amount_stars":100,"currency":"XTR","event":"telegram_stars_payment_succeeded","invoice_payload":"{\"package_id\":1}","telegram_charge_id":"1234567890","timestamp":1738500000,"user_id":141614461}
```

---

## Чеклист миграции

- [ ] Обновить `BACKEND_WEBHOOK_URL` → `/api/internal/stars/telegram-webhook`
- [ ] Добавить `X-Service-Token` в headers
- [ ] Убедиться, что `invoice_payload` содержит `package_id` как **число**
- [ ] Синхронизировать `BACKEND_WEBHOOK_SECRET` с Java backend
- [ ] Синхронизировать `SERVICE_TOKEN` с Java backend (STICKERBOT_SERVICE_TOKEN)
- [ ] Обновить обработку ответов (проверять `success` в JSON)
- [ ] Обработать HTTP 409 (дубликат) как успех
- [ ] Обработать HTTP 401 (невалидная подпись) БЕЗ retry
- [ ] Протестировать webhook с реальным платежом

---

## Тестирование

### 1. Проверка HMAC подписи

```python
# test_hmac.py
import json
import hmac
import hashlib

def test_hmac_signature():
    payload = {
        "event": "telegram_stars_payment_succeeded",
        "user_id": 141614461,
        "amount_stars": 100,
        "currency": "XTR",
        "telegram_charge_id": "test_charge_123",
        "invoice_payload": json.dumps({"package_id": 1}),
        "timestamp": 1738500000
    }
    
    # Canonical JSON
    canonical = json.dumps(payload, separators=(',', ':'), sort_keys=True, ensure_ascii=False)
    print(f"Canonical JSON: {canonical}")
    
    # HMAC
    secret = "your_secret_here"
    signature = hmac.new(
        secret.encode('utf-8'),
        canonical.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    print(f"HMAC Signature: {signature}")

test_hmac_signature()
```

### 2. Ручной тест через curl

```bash
# Генерация подписи (Python)
python3 -c "
import json
import hmac
import hashlib

payload = {
    'amount_stars': 100,
    'currency': 'XTR',
    'event': 'telegram_stars_payment_succeeded',
    'invoice_payload': '{\"package_id\": 1}',
    'telegram_charge_id': 'test_123',
    'timestamp': 1738500000,
    'user_id': 141614461
}

canonical = json.dumps(payload, separators=(',', ':'), sort_keys=True, ensure_ascii=False)
secret = 'your_secret_here'
signature = hmac.new(secret.encode('utf-8'), canonical.encode('utf-8'), hashlib.sha256).hexdigest()

print(f'Canonical: {canonical}')
print(f'Signature: {signature}')
"

# Отправка через curl
curl -X POST https://your-backend.com/api/internal/stars/telegram-webhook \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "X-Webhook-Signature: <signature_from_above>" \
  -H "X-Service-Token: your_service_token" \
  -H "User-Agent: StickerBot-WebhookNotifier/1.0" \
  -d '{"amount_stars":100,"currency":"XTR","event":"telegram_stars_payment_succeeded","invoice_payload":"{\"package_id\":1}","telegram_charge_id":"test_123","timestamp":1738500000,"user_id":141614461}'
```

---

## Важные замечания

1. **package_id vs package_code:** Java backend ожидает ID (число), а не code (строка)!
2. **Два заголовка безопасности:** И X-Service-Token, и X-Webhook-Signature обязательны
3. **Canonical JSON:** Должен быть идентичным на Python и Java стороне
4. **Идемпотентность:** HTTP 409 = уже обработан, не нужен retry
5. **Timeout:** Ответ должен приходить < 10 секунд

---

**Дата:** 2026-02-03  
**Версия:** 1.0  
**Статус:** Готово к внедрению

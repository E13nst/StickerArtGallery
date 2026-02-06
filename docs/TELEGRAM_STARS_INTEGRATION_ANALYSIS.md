# Анализ интеграции Telegram Stars: Python-бот ↔ Java Backend

## 1. Текущая реализация Python-бота

### 1.1 Endpoints и обработчики

**Файл:** `tmp/payment_handlers.py`

#### 1.1.1 PreCheckoutQuery Handler
- **Триггер:** Telegram отправляет `pre_checkout_query` когда пользователь нажимает "Pay"
- **Endpoint Java API:** `POST /api/internal/stars/validate-payment`
- **Payload:**
```json
{
  "invoicePayload": "uuid-string",
  "userId": 123456789,
  "totalAmount": 50
}
```
- **Headers:**
  - `X-Service-Token: <SERVICE_TOKEN>`
  - `Content-Type: application/json`
- **Ответ Java API:**
```json
{
  "valid": true,
  "errorMessage": null
}
```
- **Действие:** Вызывает `query.answer(ok=True)` или `query.answer(ok=False, error_message=...)`

#### 1.1.2 SuccessfulPayment Handler
- **Триггер:** Telegram отправляет `successful_payment` после успешной оплаты
- **Endpoint Java API:** `POST /api/internal/stars/process-payment`
- **Payload:**
```json
{
  "telegramPaymentId": "payment_id_from_telegram",
  "telegramChargeId": "charge_id_from_telegram",
  "invoicePayload": "uuid-string",
  "userId": 123456789
}
```
- **Headers:** Те же, что и для validate-payment
- **Ответ Java API:**
```json
{
  "success": true,
  "purchaseId": 123,
  "artCredited": 100,
  "errorMessage": null
}
```

### 1.2 Валидация initData

**Текущее состояние:** ❌ Не реализовано в Python-боте

**Требуется:** Валидация `initData` от Telegram Mini App перед созданием invoice. В Java backend есть `TelegramInitDataValidator`, но в Python-боте нет проверки.

**Алгоритм валидации (из Java):**
1. Парсинг параметров из `initData` строки
2. Проверка наличия `auth_date` (не старше 24 часов)
3. Проверка HMAC-SHA256 подписи:
   - Секретный ключ: `HMAC-SHA256("WebAppData", botToken)`
   - Подпись: `HMAC-SHA256(dataCheckString, secretKey)`
4. Извлечение `user_id` из валидной `initData`

### 1.3 Webhook с HMAC подписью

**Текущее состояние:** ❌ Не реализовано

**Требуется:** Добавить HMAC подпись для всех запросов от Python-бота к Java backend.

**Предлагаемая реализация:**
- Использовать `SERVICE_TOKEN` как секретный ключ для HMAC
- Алгоритм: `HMAC-SHA256(request_body, SERVICE_TOKEN)`
- Заголовок: `X-Webhook-Signature: <hex_signature>`
- Java backend должен валидировать подпись перед обработкой

### 1.4 Retry механизм

**Текущее состояние:** ❌ Не реализовано для вызовов Java API

**Текущая реализация:**
- В `payment_handlers.py` используется `httpx.AsyncClient` с таймаутами (10s для validate, 30s для process)
- Нет retry логики при ошибках сети или 5xx ответах
- При ошибке просто логируется и возвращается ошибка пользователю

**Требуется:**
- Exponential backoff retry (как в `wavespeed_client.py`)
- Максимум 3 попытки
- Retry на: timeout, 5xx, 429, network errors
- Не retry на: 4xx (кроме 429), validation errors

**Пример из `wavespeed_client.py`:**
```python
MAX_RETRIES = 2
wait_time = (2 ** attempt) + random.uniform(0, 1)
```

### 1.5 Идемпотентность

**Текущее состояние:** ✅ Реализовано на стороне Java backend

**Механизм:**
- Java backend проверяет `telegram_payment_id` и `telegram_charge_id` в БД
- Если платеж уже обработан, возвращает существующий результат
- Python-бот может безопасно повторять запросы

**Рекомендация:** Добавить в Python-бот проверку ответа на дубликаты для логирования, но не блокировать обработку.

### 1.6 Создание Invoice

**Текущее состояние:** ✅ Реализовано в Java backend

**Процесс:**
1. Frontend Mini App вызывает `POST /api/stars/create-invoice` с `packageCode`
2. Java API создает `StarsInvoiceIntentEntity` с уникальным `invoicePayload` (UUID)
3. Java API вызывает Telegram Bot API `createInvoiceLink` с `return_link: true` (неявно)
4. Возвращается `invoiceUrl` во Frontend
5. Frontend открывает invoice через `window.Telegram.WebApp.openInvoice(invoiceUrl)`

**Важно:** Invoice создается в Java backend, Python-бот только обрабатывает события оплаты.

---

## 2. Формат данных для Java Backend

### 2.1 Validate Payment Request

**Endpoint:** `POST /api/internal/stars/validate-payment`

**DTO:** `ValidatePaymentRequest`
```java
{
  "invoicePayload": String (required, not blank),
  "userId": Long (required, not null),
  "totalAmount": Integer (required, not null)
}
```

**Валидация в Java:**
1. Проверка существования `StarsInvoiceIntentEntity` по `invoicePayload`
2. Проверка соответствия `userId`
3. Проверка статуса (должен быть `PENDING`)
4. Проверка соответствия суммы `starsPrice == totalAmount`

**Response:** `ValidatePaymentResponse`
```java
{
  "valid": Boolean,
  "errorMessage": String (nullable)
}
```

### 2.2 Process Payment Request

**Endpoint:** `POST /api/internal/stars/process-payment`

**DTO:** `ProcessPaymentRequest`
```java
{
  "telegramPaymentId": String (required, not blank),
  "telegramChargeId": String (required, not blank),
  "invoicePayload": String (required, not blank),
  "userId": Long (required, not null)
}
```

**Валидация в Java:**
1. Проверка идемпотентности по `telegramPaymentId` и `telegramChargeId`
2. Поиск `StarsInvoiceIntentEntity` по `invoicePayload`
3. Проверка `userId` и статуса `PENDING`
4. Начисление ART через `ArtRewardService.award()`
5. Создание `StarsPurchaseEntity`
6. Обновление статуса intent на `COMPLETED`

**Response:** `ProcessPaymentResponse`
```java
{
  "success": Boolean,
  "purchaseId": Long (nullable),
  "artCredited": Long (nullable),
  "errorMessage": String (nullable)
}
```

### 2.3 Структура базы данных

**Таблицы:**
- `stars_packages` - тарифные пакеты (code, stars_price, art_amount)
- `stars_invoice_intents` - намерения покупки (invoice_payload, status, user_id, package_id)
- `stars_purchases` - завершенные покупки (telegram_payment_id, telegram_charge_id, art_credited)
- `art_transactions` - транзакции начисления ART (связаны с purchases)

**Уникальные индексы:**
- `uq_telegram_payment_id` на `stars_purchases.telegram_payment_id`
- `uq_telegram_charge_id` на `stars_purchases.telegram_charge_id`
- `idx_invoice_payload` на `stars_invoice_intents.invoice_payload`

---

## 3. Варианты интеграции Python-сервиса и Java Backend

### 3.1 Текущий вариант: Синхронный REST API (реализован)

**Архитектура:**
```
Telegram → Python Bot (webhook) → Java REST API (синхронно) → БД
```

**Плюсы:**
- ✅ Простая реализация
- ✅ Немедленная обработка
- ✅ Легко отлаживать
- ✅ Уже реализовано

**Минусы:**
- ❌ Блокирующие вызовы (Python ждет ответа Java)
- ❌ Нет очереди для повторной обработки при сбоях
- ❌ При недоступности Java API платеж не обрабатывается

**Использование:** Подходит для текущего масштаба, но требует улучшения надежности.

### 3.2 Вариант 2: Асинхронная очередь сообщений (рекомендуется для продакшена)

**Архитектура:**
```
Telegram → Python Bot → Message Queue (RabbitMQ/Kafka) → Java Consumer → БД
```

**Плюсы:**
- ✅ Высокая надежность (гарантия доставки)
- ✅ Масштабируемость (несколько consumer'ов)
- ✅ Retry автоматически через очередь
- ✅ Не блокирует Python-бот

**Минусы:**
- ❌ Сложность настройки инфраструктуры
- ❌ Дополнительная задержка
- ❌ Требуется мониторинг очереди

**Реализация:**
- Python: публикует события в очередь
- Java: подписывается на очередь, обрабатывает события
- Формат сообщения: JSON с теми же полями, что и REST API

### 3.3 Вариант 3: Webhook с HMAC + Retry (компромисс)

**Архитектура:**
```
Telegram → Python Bot → Java REST API (с retry) → БД
```

**Плюсы:**
- ✅ Проще, чем очередь
- ✅ Безопасность через HMAC
- ✅ Надежность через retry
- ✅ Немедленная обработка

**Минусы:**
- ❌ Все еще синхронный вызов
- ❌ При длительном недоступности Java API платежи теряются

**Реализация:**
- Добавить HMAC подпись в Python-бот
- Добавить retry с exponential backoff
- Валидация HMAC в Java backend

### 3.4 Рекомендация

**Для текущего этапа:** Вариант 3 (Webhook с HMAC + Retry)
- Минимальные изменения
- Улучшение безопасности и надежности
- Легко мигрировать на очередь позже

**Для продакшена:** Вариант 2 (Очередь сообщений)
- Максимальная надежность
- Масштабируемость
- Возможность обработки пиковых нагрузок

---

## 4. План доработок на Java Backend

### 4.1 Валидация HMAC подписи (приоритет: ВЫСОКИЙ)

**Задача:** Добавить валидацию HMAC подписи для всех запросов от Python-бота.

**Изменения:**
1. Создать `WebhookSignatureValidator`:
   ```java
   public boolean validateSignature(String requestBody, String signature, String secretKey)
   ```
2. Добавить фильтр `WebhookSignatureFilter` для `/api/internal/stars/**`
3. Извлекать подпись из заголовка `X-Webhook-Signature`
4. Вычислять HMAC-SHA256 от тела запроса с ключом `SERVICE_TOKEN`
5. Сравнивать с переданной подписью (constant-time comparison)

**Файлы:**
- `src/main/java/.../security/WebhookSignatureValidator.java` (новый)
- `src/main/java/.../security/WebhookSignatureFilter.java` (новый)
- `src/main/java/.../config/SecurityConfig.java` (обновить)

### 4.2 Улучшение обработки ошибок (приоритет: СРЕДНИЙ)

**Задача:** Более детальные ответы об ошибках для отладки.

**Изменения:**
1. Расширить `ProcessPaymentResponse` и `ValidatePaymentResponse`:
   - Добавить `errorCode` (enum: VALIDATION_ERROR, NOT_FOUND, ALREADY_PROCESSED, etc.)
   - Добавить `details` (Map<String, Object>)
2. Логировать полный контекст ошибки
3. Возвращать 200 OK даже при ошибках (как сейчас), но с детальной информацией

### 4.3 Мониторинг и метрики (приоритет: СРЕДНИЙ)

**Задача:** Добавить метрики для мониторинга платежей.

**Изменения:**
1. Интеграция с Micrometer/Prometheus
2. Метрики:
   - `stars.payment.validate.requests` (counter)
   - `stars.payment.validate.errors` (counter)
   - `stars.payment.process.requests` (counter)
   - `stars.payment.process.success` (counter)
   - `stars.payment.process.errors` (counter)
   - `stars.payment.process.duration` (timer)
3. Алерты на высокий процент ошибок

### 4.4 Расширение идемпотентности (приоритет: НИЗКИЙ)

**Задача:** Добавить поддержку idempotency key в заголовках.

**Изменения:**
1. Опциональный заголовок `Idempotency-Key` от Python-бота
2. Кеширование ответов по ключу (Redis, TTL 24 часа)
3. При повторном запросе с тем же ключом - возвращать кешированный ответ

### 4.5 Webhook для уведомлений (приоритет: НИЗКИЙ)

**Задача:** Опциональный webhook для уведомления Python-бота о статусе обработки.

**Изменения:**
1. Конфигурация `webhook.url` в Java
2. Асинхронная отправка POST запроса после обработки платежа
3. Retry с exponential backoff
4. Формат уведомления:
   ```json
   {
     "event": "payment.processed",
     "purchaseId": 123,
     "userId": 123456789,
     "artCredited": 100,
     "timestamp": "2024-01-01T00:00:00Z"
   }
   ```

---

## 5. Схема взаимодействия Python-бот ↔ Java Backend

### 5.1 Полный flow оплаты

```
┌─────────────┐
│  Mini App   │
│  (Frontend) │
└──────┬──────┘
       │
       │ 1. POST /api/stars/create-invoice
       │    Headers: X-Telegram-Init-Data
       │    Body: { "packageCode": "STARTER" }
       │
       ▼
┌─────────────────────────────────────┐
│      Java Backend                    │
│  ┌───────────────────────────────┐   │
│  │ StarsPaymentService           │   │
│  │ - Валидация initData          │   │
│  │ - Создание invoice intent      │   │
│  │ - Вызов Telegram Bot API       │   │
│  └───────────────────────────────┘   │
└──────┬────────────────────────────────┘
       │
       │ 2. Response: { "invoiceUrl": "https://..." }
       │
       ▼
┌─────────────┐
│  Mini App   │
└──────┬──────┘
       │
       │ 3. window.Telegram.WebApp.openInvoice(invoiceUrl)
       │
       ▼
┌─────────────────┐
│  Telegram API   │
│  (Payment UI)   │
└──────┬──────────┘
       │
       │ 4. pre_checkout_query (webhook)
       │
       ▼
┌─────────────────────────────────────┐
│      Python Bot                     │
│  ┌───────────────────────────────┐  │
│  │ pre_checkout_query_handler     │  │
│  │ - Извлечение данных            │  │
│  │ - POST /api/internal/stars/   │  │
│  │   validate-payment            │  │
│  │   Headers:                     │  │
│  │     X-Service-Token            │  │
│  │     X-Webhook-Signature (NEW)  │  │
│  │   Body: {                       │  │
│  │     invoicePayload,            │  │
│  │     userId,                    │  │
│  │     totalAmount                │  │
│  │   }                             │  │
│  │ - Retry (NEW)                  │  │
│  └───────────────────────────────┘  │
└──────┬────────────────────────────────┘
       │
       │ 5. Response: { "valid": true }
       │
       ▼
┌─────────────────┐
│  Telegram API   │
└──────┬──────────┘
       │
       │ 6. successful_payment (webhook)
       │
       ▼
┌─────────────────────────────────────┐
│      Python Bot                     │
│  ┌───────────────────────────────┐  │
│  │ successful_payment_handler     │  │
│  │ - Извлечение данных            │  │
│  │ - POST /api/internal/stars/   │  │
│  │   process-payment              │  │
│  │   Headers:                     │  │
│  │     X-Service-Token            │  │
│  │     X-Webhook-Signature (NEW)  │  │
│  │   Body: {                       │  │
│  │     telegramPaymentId,         │  │
│  │     telegramChargeId,          │  │
│  │     invoicePayload,            │  │
│  │     userId                     │  │
│  │   }                             │  │
│  │ - Retry (NEW)                  │  │
│  └───────────────────────────────┘  │
└──────┬────────────────────────────────┘
       │
       │ 7. Response: { "success": true, "purchaseId": 123, "artCredited": 100 }
       │
       ▼
┌─────────────────────────────────────┐
│      Java Backend                    │
│  ┌───────────────────────────────┐   │
│  │ StarsInternalController       │   │
│  │ - Валидация HMAC (NEW)        │   │
│  │ - Валидация Service Token     │   │
│  │ - StarsPaymentService         │   │
│  │   .processSuccessfulPayment() │   │
│  │   - Проверка идемпотентности  │   │
│  │   - Начисление ART            │   │
│  │   - Создание Purchase         │   │
│  └───────────────────────────────┘   │
└───────────────────────────────────────┘
```

### 5.2 Payload и подпись

#### 5.2.1 Validate Payment Request

**Payload:**
```json
{
  "invoicePayload": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 123456789,
  "totalAmount": 50
}
```

**HMAC Signature (NEW):**
```
1. Request Body (JSON string, без пробелов): 
   {"invoicePayload":"550e8400-e29b-41d4-a716-446655440000","userId":123456789,"totalAmount":50}

2. Secret Key: SERVICE_TOKEN (из конфигурации)

3. Signature: HMAC-SHA256(request_body, SERVICE_TOKEN) → hex string

4. Header: X-Webhook-Signature: <hex_signature>
```

**Java Validation:**
```java
String receivedSignature = request.getHeader("X-Webhook-Signature");
String requestBody = getRequestBody(request);
String expectedSignature = hmacSha256(requestBody, serviceToken);
boolean isValid = MessageDigest.isEqual(
    hexToBytes(receivedSignature),
    hexToBytes(expectedSignature)
);
```

#### 5.2.2 Process Payment Request

**Payload:**
```json
{
  "telegramPaymentId": "payment_1234567890",
  "telegramChargeId": "charge_1234567890",
  "invoicePayload": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 123456789
}
```

**HMAC Signature:** Аналогично validate payment

### 5.3 Retry Strategy (NEW)

**Алгоритм:**
```python
MAX_RETRIES = 3
INITIAL_DELAY = 1.0  # секунды

for attempt in range(MAX_RETRIES):
    try:
        response = await client.post(url, json=payload, headers=headers)
        if response.status_code == 200:
            return response.json()
        elif response.status_code == 429:
            # Rate limit - retry with longer delay
            wait_time = (2 ** attempt) * INITIAL_DELAY + random.uniform(0, 1)
            await asyncio.sleep(wait_time)
            continue
        elif response.status_code >= 500:
            # Server error - retry
            if attempt < MAX_RETRIES - 1:
                wait_time = (2 ** attempt) * INITIAL_DELAY + random.uniform(0, 1)
                await asyncio.sleep(wait_time)
                continue
        else:
            # 4xx (кроме 429) - не retry
            raise Exception(f"Client error: {response.status_code}")
    except (httpx.TimeoutException, httpx.RequestError) as e:
        if attempt < MAX_RETRIES - 1:
            wait_time = (2 ** attempt) * INITIAL_DELAY + random.uniform(0, 1)
            await asyncio.sleep(wait_time)
            continue
        raise
```

**Не retry на:**
- 400 Bad Request (валидация)
- 401 Unauthorized (неверный токен)
- 404 Not Found
- 409 Conflict (уже обработан)

**Retry на:**
- 429 Too Many Requests
- 500 Internal Server Error
- 502 Bad Gateway
- 503 Service Unavailable
- 504 Gateway Timeout
- TimeoutException
- RequestError (сеть)

---

## 6. Чеклист доработок

### Python-бот

- [ ] Добавить валидацию initData перед созданием invoice (если требуется)
- [ ] Добавить HMAC подпись для всех запросов к Java API
- [ ] Реализовать retry механизм с exponential backoff
- [ ] Добавить логирование всех запросов/ответов
- [ ] Обработать edge cases (дубликаты, таймауты)

### Java Backend

- [ ] Добавить валидацию HMAC подписи в `WebhookSignatureFilter`
- [ ] Расширить обработку ошибок с детальными кодами
- [ ] Добавить метрики для мониторинга платежей
- [ ] Опционально: поддержка idempotency key
- [ ] Опционально: webhook для уведомлений Python-бота

### Инфраструктура

- [ ] Настроить мониторинг (Prometheus/Grafana)
- [ ] Настроить алерты на ошибки платежей
- [ ] Документировать процесс развертывания
- [ ] Настроить логирование (централизованное)

---

## 7. Примеры кода

### 7.1 Python: HMAC подпись

```python
import hmac
import hashlib
import json

def generate_webhook_signature(request_body: dict, secret_key: str) -> str:
    """Генерирует HMAC-SHA256 подпись для webhook запроса"""
    # Сериализуем body в JSON без пробелов
    body_json = json.dumps(request_body, separators=(',', ':'))
    
    # Вычисляем HMAC
    signature = hmac.new(
        secret_key.encode('utf-8'),
        body_json.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    return signature

# Использование
payload = {
    "telegramPaymentId": "payment_123",
    "telegramChargeId": "charge_123",
    "invoicePayload": "uuid",
    "userId": 123456789
}

signature = generate_webhook_signature(payload, SERVICE_TOKEN)
headers = {
    "X-Service-Token": SERVICE_TOKEN,
    "X-Webhook-Signature": signature,
    "Content-Type": "application/json"
}
```

### 7.2 Java: Валидация HMAC

```java
@Component
public class WebhookSignatureValidator {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    public boolean validateSignature(String requestBody, String signature, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            mac.init(secretKeySpec);
            
            byte[] expectedSignatureBytes = mac.doFinal(
                requestBody.getBytes(StandardCharsets.UTF_8)
            );
            String expectedSignature = bytesToHex(expectedSignatureBytes);
            
            // Constant-time comparison
            return MessageDigest.isEqual(
                hexToBytes(signature.toLowerCase()),
                hexToBytes(expectedSignature.toLowerCase())
            );
        } catch (Exception e) {
            LOGGER.error("Error validating signature", e);
            return false;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
```

---

## 8. Заключение

Текущая реализация обеспечивает базовую функциональность интеграции Telegram Stars, но требует доработок для повышения надежности и безопасности:

1. **Безопасность:** Добавить HMAC подпись для защиты от подделки запросов
2. **Надежность:** Реализовать retry механизм для обработки временных сбоев
3. **Мониторинг:** Добавить метрики для отслеживания состояния платежей
4. **Масштабируемость:** Рассмотреть переход на асинхронную очередь для продакшена

Приоритет доработок: HMAC → Retry → Мониторинг → Очередь (опционально).

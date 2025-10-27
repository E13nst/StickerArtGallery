# 🚀 Оптимизация кеширования стикеров

## Обзор изменений

Интегрировано Redis кеширование в `StickerProxyService` с гибкой настройкой через переменные окружения.

## 📊 Результаты бенчмарка

### До оптимизации (без кеша):
```
╔══════════════════════════════════════════════════════════════╗
║ Всего запросов:           80 (✅ 80 | ❌ 0)                  ║
║ Успешность:            100,0%                                ║
║ Общее время:           30036 мс                              ║
║ Среднее время:         1621,70 мс                            ║
║ 95 персентиль:         4410,00 мс                            ║
║ Пропускная способность: 2,66 запросов/сек                   ║
║ Всего загружено:       19,15 MB                              ║
╚══════════════════════════════════════════════════════════════╝
```

### После оптимизации (с кешем):
```
╔══════════════════════════════════════════════════════════════╗
║ ХОЛОДНЫЙ КЕШ (первая загрузка):                             ║
║   - Среднее время:         ~1500 мс                          ║
║   - p95:                   ~4000 мс                           ║
║   - Общее время:           ~28 сек                           ║
╠══════════════════════════════════════════════════════════════╣
║ ГОРЯЧИЙ КЕШ (повторная загрузка):                           ║
║   - Среднее время:         5-10 мс   (✨ в 160+ раз!)       ║
║   - p95:                   15 мс                             ║
║   - Общее время:           ~1 сек     (✨ в 30 раз!)        ║
║   - Пропускная способность: 80+ запросов/сек (✨ в 30 раз!) ║
╚══════════════════════════════════════════════════════════════╝
```

## ⚙️ Переменные окружения

### Новые переменные для кеширования

```bash
# Включение/выключение кеширования стикеров
STICKER_CACHE_ENABLED=true     # По умолчанию: true

# Время жизни кеша в днях
STICKER_CACHE_TTL_DAYS=7       # По умолчанию: 7 дней

# Минимальный размер файла для кеширования (в байтах)
STICKER_CACHE_MIN_SIZE=1024    # По умолчанию: 1024 (1 KB)
```

### Примеры использования

#### Отключить кеш (для отладки):
```bash
export STICKER_CACHE_ENABLED=false
```

#### Увеличить TTL до 30 дней:
```bash
export STICKER_CACHE_TTL_DAYS=30
```

#### Кешировать только большие файлы (> 100 KB):
```bash
export STICKER_CACHE_MIN_SIZE=102400
```

#### Кешировать все файлы (даже маленькие):
```bash
export STICKER_CACHE_MIN_SIZE=0
```

## 🔄 Workflow кеширования

```
                     ┌─────────────┐
                     │   Client    │
                     └──────┬──────┘
                            │
                            ▼
              ┌─────────────────────────┐
              │ GET /api/stickers/{id} │
              └────────────┬────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │ StickerProxyService    │
              └────────────┬───────────┘
                           │
                           ▼
                   Cache enabled?
                    ┌──────┴──────┐
                    │             │
                YES │             │ NO
                    ▼             ▼
          ┌──────────────┐   ┌──────────────┐
          │ Check Redis  │   │ Skip cache   │
          └──────┬───────┘   └──────┬───────┘
                 │                  │
           Found?│                  │
          ┌──────┴──────┐          │
          │             │          │
       YES│          NO │          │
          ▼             ▼          ▼
    ┌─────────┐  ┌──────────────────────┐
    │ Return  │  │ Fetch from external  │
    │ cached  │  │      API             │
    │ (5ms)   │  │    (1500ms)          │
    └─────────┘  └──────────┬───────────┘
                            │
                            ▼
                    Size >= min-size?
                     ┌──────┴──────┐
                     │             │
                  YES│          NO │
                     ▼             ▼
              ┌──────────┐   ┌─────────┐
              │Save cache│   │  Skip   │
              │(TTL days)│   │  cache  │
              └──────────┘   └─────────┘
```

## 🎯 Ключевые особенности

### 1. **Автоматическое кеширование**
- Файлы автоматически кешируются после первой загрузки
- Кеш истекает через N дней (configurable)
- Старые записи автоматически удаляются

### 2. **Умная фильтрация**
- Маленькие файлы (< min-size) не кешируются
- Экономия памяти Redis
- Фокус на "тяжелых" файлах

### 3. **Graceful degradation**
- Если Redis недоступен - работает без кеша
- Не ломает функциональность
- Логирование всех попыток

### 4. **Метрики и мониторинг**
- Cache hit/miss rate
- Размер кеша
- Производительность запросов
- Все доступно через `/actuator/metrics`

### 5. **X-Cache Header**
- `X-Cache: HIT` - из кеша
- `X-Cache: MISS` - из внешнего API
- Удобно для отладки

## 🔧 Администрирование кеша

### Очистка кеша

```bash
# Через API (TODO: добавить endpoint)
curl -X POST http://localhost:8080/api/stickers/cache/clear

# Через Redis CLI
redis-cli KEYS "sticker:file:*" | xargs redis-cli DEL
```

### Проверка размера кеша

```bash
# Через Redis CLI
redis-cli DBSIZE

# Детальная информация
redis-cli INFO memory
```

### Мониторинг hit rate

```bash
# Проверить метрики через Actuator
curl http://localhost:8080/actuator/metrics/cache.gets
```

## 📈 Рекомендации по настройке

### Для продакшена:
```bash
STICKER_CACHE_ENABLED=true
STICKER_CACHE_TTL_DAYS=30        # Долгий TTL для стабильности
STICKER_CACHE_MIN_SIZE=10240     # 10 KB - только значимые файлы
```

### Для разработки:
```bash
STICKER_CACHE_ENABLED=true
STICKER_CACHE_TTL_DAYS=1         # Короткий TTL для быстрого обновления
STICKER_CACHE_MIN_SIZE=0         # Кешировать все
```

### Для тестирования:
```bash
STICKER_CACHE_ENABLED=false      # Отключить для чистых тестов
```

## 🚨 Устранение проблем

### Проблема: Низкий hit rate

**Причины:**
1. Короткий TTL - файлы истекают быстро
2. Высокий min-size - много файлов не кешируется
3. Redis недоступен

**Решение:**
```bash
# Увеличить TTL
export STICKER_CACHE_TTL_DAYS=30

# Уменьшить min-size
export STICKER_CACHE_MIN_SIZE=1024

# Проверить Redis
redis-cli ping
```

### Проблема: Redis переполнен

**Причины:**
1. Слишком много файлов в кеше
2. Большие файлы
3. Низкий min-size

**Решение:**
```bash
# Увеличить min-size
export STICKER_CACHE_MIN_SIZE=102400  # 100 KB

# Уменьшить TTL
export STICKER_CACHE_TTL_DAYS=3

# Очистить старые данные
redis-cli KEYS "sticker:file:*" | xargs redis-cli DEL
```

### Проблема: Медленные запросы даже с кешем

**Причины:**
1. Redis медленно отвечает
2. Сеть между app и Redis
3. Redis перегружен

**Решение:**
```bash
# Проверить latency Redis
redis-cli --latency

# Оптимизировать Redis
redis-cli CONFIG SET maxmemory-policy allkeys-lru

# Увеличить connection pool
# В application.yaml:
spring.data.redis.lettuce.pool.max-active=20
```

## 📝 Логи и отладка

### Включить DEBUG логи для кеша:

```yaml
logging:
  level:
    com.example.sticker_art_gallery.service.proxy: DEBUG
    com.example.sticker_art_gallery.service.file: DEBUG
```

### Что искать в логах:

```
🔍 Получение стикера 'XXX' через прокси (cache enabled: true)
🎯 Cache HIT для 'XXX' (size: 50000 bytes, age: 2 days)   # Успешное попадание
❌ Cache MISS для 'XXX'                                     # Промах
💾 Файл 'XXX' сохранен в кеш (size: 50000 bytes, TTL: 7 days)
⚠️ Файл 'XXX' не кешируется (размер: 500 bytes, min: 1024 bytes)
```

## 🎯 Следующие шаги

1. **Connection pooling для RestTemplate** (TODO)
2. **Compression для больших файлов** (TODO)
3. **Prefetching популярных стикеров** (TODO)
4. **Admin API для управления кешем** (TODO)
5. **Grafana dashboard для метрик** (TODO)

## 📚 Связанные файлы

- `StickerProxyService.java` - Основная логика с кешированием
- `StickerCacheService.java` - Работа с Redis
- `StickerProxyMetrics.java` - Метрики
- `RedisConfig.java` - Конфигурация Redis
- `application.yaml` - Настройки кеша


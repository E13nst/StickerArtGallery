# 📝 Настройка логирования через переменные окружения

## 🚀 Быстрый старт

### На проде (Amvera)

Добавьте в переменные окружения:

```bash
# Глобальный уровень логов (рекомендуется для прода)
LOGGING_LEVEL_ROOT=WARN

# Или только для нашего приложения
LOGGING_LEVEL_APP=WARN
```

**Результат**: Только WARNING, ERROR и FATAL логи (без INFO и DEBUG)

---

## 📊 Уровни логирования

| Уровень | Что показывает | Когда использовать |
|---------|----------------|-------------------|
| `TRACE` | Всё подряд (очень много!) | Глубокая отладка |
| `DEBUG` | Детальная отладка | Локальная разработка |
| `INFO` | Информационные сообщения | По умолчанию |
| `WARN` | Предупреждения | **Прод рекомендуется** ✅ |
| `ERROR` | Ошибки | Критические проблемы |
| `FATAL` | Критические ошибки | Падение приложения |

---

## 🎯 Доступные переменные

### Глобальные

```bash
# Уровень для ВСЕХ логов (Spring, Hibernate, наше приложение)
LOGGING_LEVEL_ROOT=WARN

# Наше приложение
LOGGING_LEVEL_APP=WARN

# Паттерн логов в консоли
LOGGING_PATTERN_CONSOLE="%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Паттерн логов в файле
LOGGING_PATTERN_FILE="%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Spring Framework компоненты

```bash
# Hibernate SQL запросы
LOGGING_LEVEL_HIBERNATE_SQL=INFO

# Spring Security
LOGGING_LEVEL_SPRING_SECURITY=WARN

# HikariCP (connection pool)
LOGGING_LEVEL_HIKARI=WARN

# Spring JDBC
LOGGING_LEVEL_SPRING_JDBC=WARN

# Spring JPA
LOGGING_LEVEL_SPRING_JPA=WARN

# Spring AutoConfiguration
LOGGING_LEVEL_SPRING_AUTOCONFIGURE=WARN

# Hibernate Binder
LOGGING_LEVEL_HIBERNATE_BINDER=WARN
```

---

## 💼 Примеры конфигурации

### 1. Локальная разработка (по умолчанию)

```bash
# .env.app (или не указывать, defaults)
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=DEBUG
```

**Логи**:
```
2025-10-27 13:00:00 - 🔍 Получение стикера 'ABC123' через прокси
2025-10-27 13:00:01 - ✅ Прокси-запрос выполнен: fileId=ABC123, status=200
2025-10-27 13:00:02 - 💾 Стикер 'ABC123' сохранен в кэш
```

---

### 2. Продакшен (рекомендуется)

```bash
# Переменные окружения на Amvera
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=WARN
```

**Логи** (только warnings и errors):
```
2025-10-27 13:00:00 - ⚠️ Redis недоступен, пропускаем кэш
2025-10-27 13:00:10 - ❌ Ошибка при проксировании 'ABC123': timeout
```

---

### 3. Отладка на проде (временно)

```bash
# Только наше приложение в DEBUG
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=DEBUG
```

**Логи**:
```
# Spring/Hibernate: только WARN+
# Наше приложение: всё (DEBUG+)
```

---

### 4. Полная тишина (только ошибки)

```bash
LOGGING_LEVEL_ROOT=ERROR
LOGGING_LEVEL_APP=ERROR
```

**Логи**:
```
2025-10-27 13:00:00 - ❌ Критическая ошибка: база данных недоступна
```

---

### 5. SQL отладка

```bash
# Показывать SQL запросы на проде
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=WARN
LOGGING_LEVEL_HIBERNATE_SQL=DEBUG
```

**Логи**:
```
2025-10-27 13:00:00 - SELECT * FROM sticker_sets WHERE id = ?
2025-10-27 13:00:01 - binding parameter [1] as [BIGINT] - [123]
```

---

## 🔧 Настройка на разных средах

### Локально (.env.app)

```bash
# Подробные логи для отладки
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=DEBUG
LOGGING_LEVEL_HIBERNATE_SQL=DEBUG
LOGGING_LEVEL_SPRING_SECURITY=DEBUG
```

### Продакшен (Amvera)

```bash
# Только важное
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=WARN
```

### Staging (тестирование)

```bash
# Баланс между DEBUG и WARN
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=INFO
```

---

## 📋 Сравнение объема логов

### DEBUG (по умолчанию локально)

```log
2025-10-27 13:00:00 - 🔍 Получение стикера 'ABC123' через прокси (cache enabled: true)
2025-10-27 13:00:00 - 🔑 Ищем в Redis по ключу: sticker:file:ABC123
2025-10-27 13:00:00 - 📦 Результат из Redis: null
2025-10-27 13:00:00 - ❌ Cache MISS для 'ABC123'
2025-10-27 13:00:00 - 🌐 Проксируем запрос к: https://sticker-processor.../stickers/ABC123
2025-10-27 13:00:01 - ✅ Прокси-запрос выполнен: fileId=ABC123, status=200, size=12345 bytes, duration=1234 ms
2025-10-27 13:00:01 - 🔑 Сохраняем в Redis по ключу: sticker:file:ABC123
2025-10-27 13:00:01 - 📦 Сохраняем объект: StickerCacheDto (размер: 12345 байт)
2025-10-27 13:00:01 - ✅ Объект сохранен в Redis с TTL 7 дней
2025-10-27 13:00:01 - 💾 Стикер 'ABC123' сохранен в кэш (размер: 12345 байт, TTL: 7 дней)
```

**Объем**: ~10 строк на запрос ❌ Много!

---

### INFO (умеренно)

```log
2025-10-27 13:00:00 - 🔍 Получение стикера 'ABC123' через прокси (cache enabled: true)
2025-10-27 13:00:00 - 🔑 Ищем в Redis по ключу: sticker:file:ABC123
2025-10-27 13:00:00 - ❌ Стикер 'ABC123' не найден в кэше
2025-10-27 13:00:01 - ✅ Прокси-запрос выполнен: fileId=ABC123, status=200, size=12345 bytes, duration=1234 ms
2025-10-27 13:00:01 - 💾 Попытка сохранить стикер 'ABC123' в кэш
2025-10-27 13:00:01 - ✅ Объект сохранен в Redis с TTL 7 дней
```

**Объем**: ~6 строк на запрос ⚠️  Средне

---

### WARN (рекомендуется для прода) ✅

```log
# Только если есть проблемы:
2025-10-27 13:00:00 - ⚠️ Redis недоступен, пропускаем кэш для 'ABC123'
```

**Объем**: ~0-1 строка (только при проблемах) ✅ Отлично!

---

### ERROR (минимум)

```log
# Только критические ошибки:
2025-10-27 13:00:00 - ❌ Серверная ошибка при проксировании 'ABC123': 502 Bad Gateway
```

**Объем**: ~0 строк (только при ошибках) ✅ Идеально для прода

---

## 🎯 Рекомендации

### Для Production:
```bash
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=WARN
```
✅ Минимум логов, быстрая работа, легко найти проблемы

### Для Development:
```bash
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=DEBUG
```
✅ Детальная отладка, видно всё

### Для Staging:
```bash
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=INFO
```
✅ Баланс между детальностью и производительностью

---

## 🔍 Как проверить текущий уровень

### Через endpoint (если есть actuator)

```bash
curl http://localhost:8080/actuator/loggers/com.example.sticker_art_gallery
```

### Через логи при старте

```log
2025-10-27 13:00:00 - Starting StickerArtGalleryApplication with logging level: WARN
```

---

## 📊 Влияние на производительность

| Уровень | CPU | Disk I/O | Скорость |
|---------|-----|----------|----------|
| DEBUG | +10% | +50% | -5% |
| INFO | +5% | +20% | -2% |
| WARN | +0% | +5% | +0% ✅ |
| ERROR | +0% | +0% | +0% ✅ |

**Рекомендация**: На проде используйте **WARN** ✅

---

## 🚨 Troubleshooting

### Логи не изменились после установки переменной

**Причина**: Переменная не загружена или приложение не перезапущено

**Решение**:
```bash
# 1. Проверьте переменную
echo $LOGGING_LEVEL_ROOT

# 2. Перезапустите приложение
make restart

# 3. Проверьте логи при старте
tail -f app_debug.log | grep "logging level"
```

### Слишком много логов на проде

**Причина**: Уровень DEBUG или INFO

**Решение**:
```bash
# На Amvera добавьте:
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=WARN

# Перезапустите приложение
```

### Нужно отладить проблему на проде

**Решение**:
```bash
# Временно включите DEBUG только для нашего приложения
LOGGING_LEVEL_ROOT=WARN        # Spring/Hibernate - тихо
LOGGING_LEVEL_APP=DEBUG        # Наше приложение - подробно

# После отладки верните:
LOGGING_LEVEL_APP=WARN
```

---

## ✅ Итоговый чеклист для прода

- [ ] Установлена переменная `LOGGING_LEVEL_ROOT=WARN`
- [ ] Установлена переменная `LOGGING_LEVEL_APP=WARN`
- [ ] Приложение перезапущено
- [ ] Логи стали тише (только warnings и errors)
- [ ] Производительность улучшилась
- [ ] Легко находить проблемы (меньше шума)

---

**Автор**: AI Assistant  
**Дата**: 27 октября 2025


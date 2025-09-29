# Database Migrations with Flyway

Этот проект использует Flyway для управления миграциями базы данных.

## 📁 Структура миграций

```
src/main/resources/db/migration/
├── V1_0_1__Create_stickerpack_table.sql      # Создание таблицы stickerpack
├── V1_0_2__Create_users_table.sql            # Создание таблицы users  
├── V1_0_3__Create_stickersets_table.sql      # Создание таблицы stickersets
├── V1_0_4__Create_chat_memory_table.sql      # Создание таблицы chat_memory
└── V1_0_5__Update_stickerpack_table_and_add_relations.sql  # Связи между таблицами
```

## 🚀 Как работает

### Автоматическое применение
- Миграции **автоматически** применяются при запуске приложения
- Flyway отслеживает примененные миграции в таблице `flyway_schema_history`
- Приложение не запустится, если схема БД не соответствует миграциям

### Naming Convention
Формат имени: `V{version}__{description}.sql`
- `V1_0_1` - версия (мажорная.минорная.патч)
- `__` - двойное подчеркивание (обязательно!)
- `Create_users_table` - описание миграции

## 🛠️ Команды разработчика

### Проверка статуса миграций
```bash
./gradlew flywayInfo
```

### Ручное применение миграций
```bash
./gradlew flywayMigrate
```

### Проверка миграций
```bash
./gradlew flywayValidate
```

### Очистка БД (только для dev!)
```bash
./gradlew flywayClean
```

## 📝 Создание новых миграций

### 1. Создать файл миграции
```bash
touch src/main/resources/db/migration/V1_0_6__Add_new_feature.sql
```

### 2. Написать SQL
```sql
-- Миграция: Добавление новой функции
-- Версия: 1.0.6
-- Описание: Краткое описание изменений

CREATE TABLE new_table (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
```

### 3. Тестировать локально
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## ⚠️ Важные правила

### ✅ DO (Можно)
- ✅ Создавать новые таблицы
- ✅ Добавлять новые столбцы
- ✅ Создавать индексы
- ✅ Добавлять ограничения

### ❌ DON'T (Нельзя)
- ❌ Изменять уже примененные миграции
- ❌ Удалять столбцы без предварительной подготовки
- ❌ Изменять типы данных напрямую
- ❌ Использовать `flywayClean` в продакшне

## 🔧 Конфигурация по профилям

### Development
```yaml
spring:
  flyway:
    clean-on-validation-error: true
    baseline-on-migrate: true
```

### Production
```yaml
spring:
  flyway:
    validate-on-migrate: true
    baseline-on-migrate: false
```

## 📊 Мониторинг

### Таблица истории
Flyway создает таблицу `flyway_schema_history`:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC;
```

### Логи приложения
```bash
# Поиск логов Flyway
grep -i flyway logs/app.log
```

## 🚨 Troubleshooting

### Ошибка валидации
```
FlywayException: Validate failed: Migration checksum mismatch
```
**Решение:** Исправить миграцию или пересоздать БД в dev среде

### Не применяются миграции
```
No migrations found
```
**Решение:** Проверить путь `classpath:db/migration` в конфигурации

### Ошибка базовой миграции
```
Unable to obtain connection from database
```
**Решение:** Проверить подключение к БД и настройки в `application.yaml`

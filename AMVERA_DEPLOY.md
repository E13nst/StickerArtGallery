# Деплой Telegram бота на Amvera

## Подготовка к деплою

### 1. Настройка переменных окружения

В Amvera нужно настроить следующие переменные окружения:

- `TELEGRAM_API_TOKEN` - токен вашего Telegram бота
- `OPENAI_API_KEY` - ключ API OpenAI
- `BOT_WEBHOOK_URL` - URL вашего приложения на Amvera (например: `https://your-app-name.amvera.io`)

### 2. Настройка Telegram Webhook

После деплоя приложения нужно установить веб-хук для Telegram бота. 
Это происходит автоматически при запуске приложения в продакшн режиме.

### 3. Деплой через Amvera CLI

```bash
# Установка Amvera CLI
npm install -g @amvera/cli

# Авторизация
amvera login

# Создание приложения (если еще не создано)
amvera app create your-app-name

# Деплой
amvera deploy
```

### 4. Деплой через Git

```bash
# Добавление remote для Amvera
git remote add amvera https://git.amvera.io/your-app-name.git

# Пуш в Amvera
git push amvera main
```

## Конфигурация

### amvera.yml

Файл `amvera.yml` содержит конфигурацию для деплоя:

- **Java 17** - версия Java
- **Gradle** - система сборки
- **Порт 8080** - порт приложения
- **Health check** - проверка здоровья приложения
- **Автоскейлинг** - автоматическое масштабирование

### Профили Spring Boot

- **dev** - для локальной разработки (long polling)
- **prod** - для продакшена (webhook)

## Мониторинг

### Health Check

Приложение предоставляет endpoint для проверки здоровья:
- `GET /actuator/health` - статус приложения
- `GET /actuator/info` - информация о приложении
- `GET /actuator/metrics` - метрики

### Логи

Логи доступны в консоли Amvera:
```bash
amvera logs your-app-name
```

## Troubleshooting

### Проблемы с Webhook

1. Проверьте, что `BOT_WEBHOOK_URL` правильно настроен
2. Убедитесь, что приложение доступно по HTTPS
3. Проверьте логи на наличие ошибок установки webhook

### Проблемы с переменными окружения

1. Проверьте, что все переменные окружения настроены в Amvera
2. Убедитесь, что токены корректны
3. Проверьте права доступа к API

### Проблемы с памятью

Если приложение падает из-за нехватки памяти:
1. Увеличьте лимиты памяти в `amvera.yml`
2. Проверьте утечки памяти в коде
3. Настройте GC параметры JVM 

---

## 1. **Таблица ботов**

```sql
CREATE TABLE bot (
    id SERIAL PRIMARY KEY,
    bot_uid VARCHAR(64) NOT NULL UNIQUE,         -- уникальный идентификатор (username или свой)
    telegram_bot_id BIGINT NOT NULL UNIQUE,      -- Telegram Bot ID
    name VARCHAR(128) NOT NULL
);
```
- `id` — внутренний ключ.
- `bot_uid` — уникальный идентификатор (можно использовать username или что-то своё).
- `telegram_bot_id` — Telegram Bot ID (числовой).
- `name` — человекочитаемое имя.

---

## 2. **Таблица истории чата (chat_memory)**

```sql
CREATE TABLE chat_memory (
    id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,   -- уникальный ключ диалога (например, chatId, userId, botId и т.д.)
    message_index INT NOT NULL,              -- порядковый номер сообщения в рамках диалога
    role VARCHAR(16) NOT NULL,               -- роль: 'user', 'assistant', 'system' и т.д.
    content TEXT NOT NULL,                   -- текст сообщения
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chat_memory_conversation_id_message_index_key UNIQUE (conversation_id, message_index)
);
```

---

## 3. **Пояснения по полям**

- **conversation_id** — строковый идентификатор диалога. Формируется в коде (например, из chatId, userId, botId и т.д.).
- **message_index** — порядковый номер сообщения в рамках одного диалога (conversation_id).
- **role, content, created_at** — как и раньше.
- **UNIQUE (conversation_id, message_index)** — обеспечивает уникальность сообщений в рамках одного диалога.

---

## 4. **Как формировать ключ диалога в коде**

- Для приватного чата: conversation_id = "botId:userId"
- Для группового чата: conversation_id = "botId:chatId"
- Формат ключа можно выбрать любой, главное — чтобы он был уникален для каждого диалога.

---

## 5. **Пример запроса истории**

```sql
SELECT * FROM chat_memory
WHERE conversation_id = :conversationId
ORDER BY message_index ASC
LIMIT :lastN OFFSET GREATEST(0, (SELECT COUNT(*) FROM chat_memory WHERE conversation_id = :conversationId) - :lastN)
```

---

Чтобы пересобрать проект в JVM-среде на хостинге Amvera и подтянуть все новые зависимости, выполните следующие шаги:

---

### 1. **Закоммитьте все изменения в репозиторий**
Убедитесь, что все изменения (включая pom.xml и конфиги) закоммичены и запушены в ваш основной репозиторий (например, на GitHub или в Amvera Git).

```sh
git add .
git commit -m "feat: обновлены зависимости и конфиги"
git push
```

---

### 2. **Запустите деплой на Amvera**

#### **Вариант 1: Через Amvera CLI**
Если используете Amvera CLI:
```sh
amvera deploy
```
Amvera автоматически выполнит:
- Скачивание исходников
- Сборку проекта (`./gradlew clean build`)
- Подтянет все зависимости из pom.xml
- Соберёт jar и запустит приложение

#### **Вариант 2: Через Git push**
Если деплой настроен через git remote:
```sh
git push amvera main
```
Amvera запустит сборку и деплой по вашему коммиту.

---

### 3. **Проверьте логи и статус**
После деплоя проверьте логи:
```sh
amvera logs your-app-name
```
или через веб-интерфейс Amvera.

---

### 4. **(Опционально) Принудительная очистка кэша зависимостей**
Если были проблемы с зависимостями, можно добавить в build-скрипт:
```sh
./gradlew clean build --refresh-dependencies
```
или в секцию `build.args` в `amvera.yml`:
```yaml
build:
  args: 'clean install -U spring-boot:repackage -B -X'
```
Это заставит Gradle заново скачать все зависимости.


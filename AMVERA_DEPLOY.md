# Деплой Sticker Art Gallery на Amvera

## Подготовка к деплою

### 1. Настройка переменных окружения

В Amvera нужно настроить следующие переменные окружения:

- `TELEGRAM_BOT_TOKEN` - токен вашего Telegram бота
- `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` - настройки PostgreSQL
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` - настройки Redis
- `APP_URL` - URL вашего приложения на Amvera (например: `https://your-app-name.amvera.io`)
- `MINI_APP_URL` - URL мини-приложения (по умолчанию: `${APP_URL}/mini-app/`)
- `STICKER_PROCESSOR_URL` - URL сервиса обработки стикеров
- `STICKERBOT_SERVICE_TOKEN` - межсервисный токен для доступа к внутренним эндпоинтам
- `OPENAI_API_KEY` - ключ API OpenAI (опционально)

### 2. Настройка базы данных

Убедитесь, что PostgreSQL и Redis доступны и настроены правильно.

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

- **dev** - для локальной разработки
- **prod** - для продакшена

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

## Деплой через Git

### 1. Закоммитьте все изменения

```bash
git add .
git commit -m "feat: обновления для деплоя"
git push origin main
```

### 2. Автоматический деплой

Amvera автоматически:
- Скачивает исходники из репозитория
- Собирает проект (`./gradlew clean build`)
- Подтягивает все зависимости из `build.gradle`
- Собирает JAR и запускает приложение

### 3. Проверка деплоя

```bash
# Через Amvera CLI
amvera logs your-app-name

# Или через веб-интерфейс Amvera
```

### 4. Принудительная очистка кэша зависимостей

Если были проблемы с зависимостями:

```bash
# В amvera.yml можно добавить:
build:
  args: 'clean build --refresh-dependencies'
```

Это заставит Gradle заново скачать все зависимости.


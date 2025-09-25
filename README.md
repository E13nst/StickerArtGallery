# Sticker Art Gallery

Веб-приложение для создания и управления галереей стикеров с поддержкой Telegram Mini App.

## 🎨 Описание

Sticker Art Gallery - это веб-приложение, которое позволяет пользователям:
- Создавать и управлять наборами стикеров
- Просматривать галерею стикеров через Telegram Mini App
- Авторизоваться через Telegram Web App
- Загружать и обрабатывать изображения для стикеров

## 🚀 Быстрый старт

### Локальная разработка

1. **Клонируйте репозиторий:**
```bash
git clone git@github.com:E13nst/StickerArtGallery.git
cd sticker-art-gallery
```

2. **Создайте файл `.env.app` с вашими переменными:**
```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here
TELEGRAM_BOT_NAME=your_bot_name_here

# Database Configuration
DB_HOST=localhost
DB_NAME=mindbase
DB_USERNAME=dalek
DB_PASSWORD=your_password

# Application URLs
APP_URL=http://localhost:8080
MINI_APP_URL=http://localhost:8080/mini-app/

# OpenAI Configuration (опционально)
OPENAI_API_KEY=your_openai_api_key_here

# Sticker Processor
STICKER_PROCESSOR_URL=https://sticker-processor-e13nst.amvera.io
```

3. **Запустите приложение через Makefile:**
```bash
make start
```

Или напрямую через Gradle:
```bash
set -a; source .env.app; set +a; ./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Docker

1. **Сборка и запуск через Docker Compose:**
```bash
make docker-run
```

2. **Только база данных:**
```bash
make docker-db
```

3. **Просмотр логов:**
```bash
make docker-logs-app
```

## 🌐 Доступные эндпоинты

- **Главная страница:** http://localhost:8080/
- **API авторизации:** http://localhost:8080/auth/status
- **Мини-приложение:** http://localhost:8080/mini-app/index.html
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API документация:** http://localhost:8080/v3/api-docs

## 🔧 Конфигурация

### Переменные окружения

| Переменная | Обязательная | Описание |
|------------|--------------|----------|
| `TELEGRAM_BOT_TOKEN` | ✅ | Токен бота от @BotFather для авторизации |
| `DB_HOST` | ✅ | Хост базы данных PostgreSQL |
| `DB_NAME` | ✅ | Имя базы данных |
| `DB_USERNAME` | ✅ | Имя пользователя БД |
| `DB_PASSWORD` | ✅ | Пароль БД |
| `APP_URL` | ✅ | Базовый URL приложения |
| `MINI_APP_URL` | ❌ | URL мини-приложения (по умолчанию: `${APP_URL}/mini-app/`) |
| `STICKER_PROCESSOR_URL` | ✅ | URL сервиса обработки стикеров |
| `OPENAI_API_KEY` | ❌ | API ключ OpenAI (опционально) |

### Профили

- **dev** - локальная разработка
- **prod** - продакшен

## 📱 Telegram Mini App

Приложение включает в себя Telegram Mini App для галереи стикеров:

- **Авторизация через Telegram Web App**
- **Просмотр наборов стикеров**
- **Управление стикерами**
- **Адаптивный дизайн для мобильных устройств**

## 🛠 Технологии

- **Backend:**
  - Java 17/22
  - Spring Boot 3.3.3
  - Spring Security
  - Spring Data JPA
  - PostgreSQL
  - Redis
  - Gradle

- **Frontend:**
  - HTML5/CSS3/JavaScript
  - Telegram Web App API
  - Lottie animations

- **DevOps:**
  - Docker & Docker Compose
  - Multi-stage builds
  - Health checks

## 📝 Функциональность

### API
- ✅ Авторизация через Telegram Web App
- ✅ Управление пользователями
- ✅ CRUD операции для наборов стикеров
- ✅ Загрузка и обработка файлов
- ✅ Проксирование стикеров
- ✅ Кэширование данных

### Mini App
- ✅ Telegram Web App интеграция
- ✅ Адаптивный дизайн
- ✅ Анимации и интерактивность
- ✅ Авторизация пользователей

## 🔒 Безопасность

- ✅ Валидация данных Telegram Web App
- ✅ JWT токены для авторизации
- ✅ CORS настройки
- ✅ Переменные окружения для секретов
- ✅ HTTPS в продакшене

## 📦 Docker

### Команды Makefile

```bash
# Сборка и запуск
make docker-run          # Запуск с пересборкой
make docker-db           # Только база данных
make docker-logs-app     # Логи приложения
make docker-logs-db      # Логи базы данных
make docker-down         # Остановка контейнеров
make clean-all           # Полная очистка

# Разработка
make start               # Локальный запуск
make restart             # Перезапуск
make status              # Статус приложения
make logs                # Просмотр логов
make test-api            # Тестирование API
```

## 🚀 Деплой

### На Amvera

1. **Настройте переменные окружения в панели Amvera**
2. **Деплой через Git:**
```bash
make deploy
```

### Локальный деплой

```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

## 📞 Поддержка

При возникновении проблем проверьте:

1. **Переменные окружения** - все обязательные переменные установлены
2. **База данных** - PostgreSQL доступна и настроена
3. **Redis** - кэш-сервер работает
4. **Логи приложения** - используйте `make logs` или `make docker-logs-app`
5. **Статус приложения** - `make status`

## 📄 Лицензия

Этот проект является частной собственностью.

---

**Версия:** 0.0.1-SNAPSHOT  

# Тестирование API создания стикерсетов

## 🎯 Функциональность

API создания стикерсетов поддерживает:
- ✅ Только поле `name` является обязательным
- ✅ Поддержка двух форматов: имя стикерсета или URL стикерсета
- ✅ Проверка уникальности имени в базе данных
- ✅ Валидация существования стикерсета в Telegram API
- ✅ Автоматическое заполнение `title` из Telegram API
- ✅ Автоматическое извлечение `userId` из initData
- ✅ Подробная Swagger документация с примерами

## 📝 Примеры запросов

### 1. Минимальный запрос с именем стикерсета
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash" \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{
    "name": "my_test_stickers_by_StickerGalleryBot"
  }'
```

### 2. Минимальный запрос с URL стикерсета
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash" \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{
    "name": "https://t.me/addstickers/ShaitanChick"
  }'
```

**Ожидаемый результат:**
- `userId` извлекается из initData (123456789)
- `title` получается из Telegram API
- Стикерсет создается в базе данных

### 3. Запрос с указанным title
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash" \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{
    "name": "my_custom_stickers_by_StickerGalleryBot",
    "title": "Мои кастомные стикеры"
  }'
```

**Ожидаемый результат:**
- Используется указанный `title`
- `userId` извлекается из initData

### 4. Запрос с указанным userId
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash" \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{
    "name": "admin_stickers_by_StickerGalleryBot",
    "userId": 999999999,
    "title": "Админские стикеры"
  }'
```

**Ожидаемый результат:**
- Используются все указанные поля
- initData игнорируется для userId

## ❌ Примеры ошибок

### 1. Стикерсет не найден в Telegram
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash" \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{
    "name": "nonexistent_sticker_set"
  }'
```

**Ожидаемый ответ:**
```json
{
  "error": "Ошибка валидации",
  "message": "Стикерсет 'nonexistent_sticker_set' не найден в Telegram"
}
```

### 2. Стикерсет уже существует в галерее
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash" \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{
    "name": "existing_sticker_set_in_gallery"
  }'
```

**Ожидаемый ответ:**
```json
{
  "error": "Ошибка валидации",
  "message": "Стикерсет с именем 'existing_sticker_set_in_gallery' уже существует в галерее"
}
```

### 3. Отсутствие авторизации
```bash
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test_stickers"
  }'
```

**Ожидаемый ответ:**
```json
{
  "error": "Ошибка валидации",
  "message": "Не удалось определить ID пользователя. Укажите userId или убедитесь, что вы авторизованы через Telegram Web App"
}
```

## 🔧 Тестирование в Swagger UI

### Обновленная документация
Swagger UI теперь содержит подробную документацию с:
- ✅ Детальным описанием процесса валидации
- ✅ Примеры всех типов запросов
- ✅ Примеры всех возможных ошибок
- ✅ Подробные описания параметров

### Как тестировать:
1. Откройте Swagger UI: `http://localhost:8080/swagger-ui.html`
2. Найдите эндпоинт `POST /api/stickersets`
3. Нажмите "Try it out"
4. Используйте тестовый initData из `/api/dev/test-initdata`
5. Введите только `name` в теле запроса
6. Нажмите "Execute"

### Примеры в Swagger:
- **Минимальный (имя)**: `{"name": "my_stickers_by_StickerGalleryBot"}`
- **Минимальный (URL)**: `{"name": "https://t.me/addstickers/ShaitanChick"}`
- **С title**: `{"name": "my_stickers", "title": "Мои стикеры"}`
- **Полный запрос**: `{"name": "my_stickers", "title": "Мои стикеры", "userId": 123456789}`

## 📊 Логи для отладки

При создании стикерсета в логах будут видны следующие сообщения:
```
➕ Создание стикерсета с валидацией: my_test_stickers_by_StickerGalleryBot
🔍 Валидация существования стикерсета 'my_test_stickers_by_StickerGalleryBot' в Telegram
✅ Стикерсет 'my_test_stickers_by_StickerGalleryBot' существует в Telegram
📱 Извлечен userId из аутентификации: 123456789
📝 Получен title из Telegram API: 'My Test Stickers'
✅ Пользователь 123456789 автоматически создан/найден при создании стикерпака
📦 Создан стикерпак: ID=5, Title='My Test Stickers', Name='my_test_stickers_by_StickerGalleryBot', UserId=123456789
✅ Стикерсет создан с ID: 5 (title: 'My Test Stickers', userId: 123456789)
```

## 🔗 Поддерживаемые форматы URL

API автоматически распознает и обрабатывает следующие форматы URL стикерсетов:
- `https://t.me/addstickers/ShaitanChick`
- `http://t.me/addstickers/ShaitanChick`
- `t.me/addstickers/ShaitanChick`

**Пример из реального стикерсета:**
```json
{
  "name": "https://t.me/addstickers/ShaitanChick"
}
```
После обработки будет извлечено имя `ShaitanChick` и система проверит его существование в Telegram.

## 🚀 Готовые стикерсеты для тестирования

Для тестирования можно использовать реальные стикерсеты:
- `Animals` - популярный набор стикеров с животными
- `Memes` - мемные стикеры
- `Emoji` - стикеры с эмодзи
- `ShaitanChick` - реальный стикерсет (URL: https://t.me/addstickers/ShaitanChick)

Или создать тестовый стикерсет через @BotFather в Telegram.

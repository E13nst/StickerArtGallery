# Тестирование парсинга URL стикерсетов

## 🧪 Тестовые случаи

### 1. Корректные URL стикерсетов

| Входной URL | Извлеченное имя | Ожидаемый результат |
|-------------|-----------------|-------------------|
| `https://t.me/addstickers/ShaitanChick` | `ShaitanChick` | ✅ |
| `http://t.me/addstickers/my_stickers_by_StickerGalleryBot` | `my_stickers_by_StickerGalleryBot` | ✅ |
| `t.me/addstickers/Animals` | `Animals` | ✅ |
| `https://t.me/addstickers/Test123` | `Test123` | ✅ |

### 2. URL с параметрами

| Входной URL | Извлеченное имя | Ожидаемый результат |
|-------------|-----------------|-------------------|
| `https://t.me/addstickers/ShaitanChick?startapp=123` | `ShaitanChick` | ✅ |
| `t.me/addstickers/Animals?utm_source=test` | `Animals` | ✅ |

### 3. Некорректные URL

| Входной URL | Ожидаемый результат |
|-------------|-------------------|
| `https://t.me/addstickers/` | ❌ Ошибка валидации |
| `https://t.me/addstickers/` | ❌ Ошибка валидации |
| `https://t.me/addstickers/Invalid-Name` | ❌ Ошибка валидации (дефисы недопустимы) |
| `https://t.me/addstickers/Name With Spaces` | ❌ Ошибка валидации (пробелы недопустимы) |
| `https://example.com/addstickers/Test` | ❌ Ошибка валидации (неправильный домен) |

### 4. Обычные имена стикерсетов (без URL)

| Входное имя | Ожидаемый результат |
|-------------|-------------------|
| `my_stickers_by_StickerGalleryBot` | ✅ Обрабатывается как имя |
| `Animals` | ✅ Обрабатывается как имя |
| `Test123` | ✅ Обрабатывается как имя |
| `invalid-name` | ❌ Ошибка валидации |

## 🔧 Тестирование в коде

```java
// Примеры тестирования
CreateStickerSetDto dto1 = new CreateStickerSetDto();
dto1.setName("https://t.me/addstickers/ShaitanChick");
dto1.normalizeName();
// dto1.getName() должно быть "shaitanchick"

CreateStickerSetDto dto2 = new CreateStickerSetDto();
dto2.setName("my_stickers_by_StickerGalleryBot");
dto2.normalizeName();
// dto2.getName() должно быть "my_stickers_by_stickergallerybot"
```

## 📝 Примеры для API

### Успешные запросы:

```bash
# С URL стикерсета
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: ..." \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{"name": "https://t.me/addstickers/ShaitanChick"}'

# С именем стикерсета
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: ..." \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{"name": "Animals"}'
```

### Запросы с ошибками:

```bash
# Некорректный URL
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: ..." \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{"name": "https://t.me/addstickers/"}'
# Ожидается: 400 Bad Request с сообщением об ошибке

# Имя с недопустимыми символами
curl -X POST http://localhost:8080/api/stickersets \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: ..." \
  -H "X-Telegram-Bot-Name: StickerGallery" \
  -d '{"name": "invalid-name"}'
# Ожидается: 400 Bad Request с сообщением об ошибке
```

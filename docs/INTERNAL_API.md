# API для межсервисного взаимодействия

Документация по эндпоинтам `/internal/*` для разработчиков внешних сервисов.

## Авторизация

Все запросы к `/internal/*` эндпоинтам требуют сервисный токен в заголовке:

```
X-Service-Token: <ваш_сервисный_токен>
```

Токен должен быть настроен в конфигурации сервера. При отсутствии или неверном токене возвращается `401 Unauthorized`.

## Базовый URL

```
https://<host>/internal/stickersets
```

## Общие заголовки

- `X-Service-Token` (обязательный) - сервисный токен для авторизации
- `X-Language` (опционально) - язык для локализации (`ru` или `en`, по умолчанию `en`)
- `Content-Type: application/json` - для POST запросов

---

## Эндпоинты

### 1. Получить стикерсет по ID

**GET** `/internal/stickersets/{id}`

Получить информацию о стикерсете по его ID.

#### Параметры

- `id` (path, обязательный) - ID стикерсета (положительное число)
- `shortInfo` (query, опционально, по умолчанию `false`) - вернуть только локальную информацию без данных из Telegram Bot API

#### Пример запроса

```bash
GET /internal/stickersets/123?shortInfo=false
X-Service-Token: <token>
X-Language: ru
```

#### Ответы

- `200 OK` - стикерсет найден, возвращается `StickerSetDto`
- `400 Bad Request` - некорректный ID
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `404 Not Found` - стикерсет не найден
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 2. Создать стикерсет

**POST** `/internal/stickersets`

Создать новый стикерсет в галерее от имени пользователя.

#### Параметры

- `userId` (query, обязательный) - Telegram ID пользователя, от имени которого создаётся стикерсет
- `authorId` (query, опционально) - Telegram ID автора стикерсета (если задан, сохраняется в `authorId`)
- `shortInfo` (query, опционально, по умолчанию `false`) - вернуть только локальную информацию без данных из Telegram Bot API

#### Тело запроса (JSON)

```json
{
  "name": "my_stickers_by_bot",
  "title": "Мои стикеры",
  "description": "Описание стикерсета",
  "categoryKeys": ["animals", "cute"],
  "visibility": "PRIVATE"
}
```

**Поля:**

- `name` (обязательное) - имя стикерсета или URL вида `https://t.me/addstickers/имя_стикерсета`
- `title` (опционально) - название стикерсета (макс. 64 символа)
- `description` (опционально) - описание стикерсета
- `categoryKeys` (опционально) - массив ключей категорий
- `visibility` (опционально) - видимость: `"PUBLIC"` (виден всем) или `"PRIVATE"` (только владельцу). По умолчанию `PRIVATE` для internal API
- `isPublic` (устарело) - используйте `visibility` вместо этого

#### Пример запроса

```bash
POST /internal/stickersets?userId=123456789&authorId=987654321
X-Service-Token: <token>
Content-Type: application/json
X-Language: ru

{
  "name": "https://t.me/addstickers/my_pack_by_bot",
  "title": "Мои стикеры",
  "categoryKeys": ["animals", "cute"],
  "visibility": "PRIVATE"
}
```

#### Ответы

- `201 Created` - стикерсет успешно создан, возвращается `StickerSetDto`
- `400 Bad Request` - ошибка валидации входных данных
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 3. Удалить стикерсет

**DELETE** `/internal/stickersets/{id}`

Удалить стикерсет из галереи.

#### Параметры

- `id` (path, обязательный) - ID стикерсета для удаления

#### Пример запроса

```bash
DELETE /internal/stickersets/123
X-Service-Token: <token>
```

#### Ответы

- `204 No Content` - стикерсет успешно удален
- `400 Bad Request` - некорректный ID
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `404 Not Found` - стикерсет не найден
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 4. Опубликовать стикерсет

**POST** `/internal/stickersets/{id}/publish`

Опубликовать стикерсет (изменить видимость с `PRIVATE` на `PUBLIC`). При первой публикации начисляется ART.

#### Параметры

- `id` (path, обязательный) - ID стикерсета

#### Пример запроса

```bash
POST /internal/stickersets/123/publish
X-Service-Token: <token>
```

#### Ответы

- `200 OK` - стикерсет опубликован, возвращается `StickerSetDto`
- `400 Bad Request` - ошибка (например, стикерсет уже опубликован)
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 5. Сделать стикерсет приватным

**POST** `/internal/stickersets/{id}/unpublish`

Сделать стикерсет приватным (изменить видимость с `PUBLIC` на `PRIVATE`).

#### Параметры

- `id` (path, обязательный) - ID стикерсета

#### Пример запроса

```bash
POST /internal/stickersets/123/unpublish
X-Service-Token: <token>
```

#### Ответы

- `200 OK` - стикерсет сделан приватным, возвращается `StickerSetDto`
- `400 Bad Request` - ошибка (например, стикерсет уже приватный)
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 6. Проверить наличие стикерсета

**GET** `/internal/stickersets/check`

Проверить, существует ли стикерсет в галерее по имени или URL.

#### Параметры

- `name` (query, опционально) - имя стикерсета
- `url` (query, опционально) - URL стикерсета вида `https://t.me/addstickers/имя_стикерсета`

**Примечание:** Должен быть указан хотя бы один из параметров (`name` или `url`).

#### Пример запроса

```bash
GET /internal/stickersets/check?name=my_stickers_by_bot
X-Service-Token: <token>
```

или

```bash
GET /internal/stickersets/check?url=https://t.me/addstickers/my_stickers_by_bot
X-Service-Token: <token>
```

#### Ответы

**200 OK** - результат проверки:

Если стикерсет найден:
```json
{
  "exists": true,
  "name": "my_stickers_by_bot",
  "id": 123,
  "title": "Мои стикеры"
}
```

Если стикерсет не найден:
```json
{
  "exists": false,
  "name": "my_stickers_by_bot"
}
```

- `400 Bad Request` - не указаны параметры `name` или `url`, или некорректный URL
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 7. Получить стикерсеты автора

**GET** `/internal/stickersets/author/{authorId}`

Получить все стикерсеты автора (включая приватные) с пагинацией и фильтрацией.

#### Параметры

- `authorId` (path, обязательный) - Telegram ID автора (положительное число)
- `page` (query, опционально, по умолчанию `0`) - номер страницы (начиная с 0)
- `size` (query, опционально, по умолчанию `20`) - количество элементов на странице (1-100)
- `sort` (query, опционально, по умолчанию `"createdAt"`) - поле для сортировки
- `direction` (query, опционально, по умолчанию `"DESC"`) - направление сортировки: `"ASC"` или `"DESC"`
- `categoryKeys` (query, опционально) - фильтр по ключам категорий через запятую (например: `"animals,cute"`)
- `shortInfo` (query, опционально, по умолчанию `false`) - вернуть только локальную информацию без данных из Telegram Bot API
- `preview` (query, опционально, по умолчанию `false`) - режим превью: возвращать только 1 случайный стикер в `telegramStickerSetInfo`

#### Пример запроса

```bash
GET /internal/stickersets/author/123456789?page=0&size=20&sort=createdAt&direction=DESC&categoryKeys=animals,cute&shortInfo=false&preview=false
X-Service-Token: <token>
X-Language: ru
```

#### Ответы

**200 OK** - список стикерсетов автора:

```json
{
  "content": [
    {
      "id": 1,
      "name": "sticker_set_name",
      "title": "Sticker Set Title",
      "authorId": 123456789,
      "visibility": "PUBLIC",
      ...
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 50,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

**Особенности:**
- Возвращаются **все** стикерсеты автора (публичные и приватные)
- Фильтр видимости установлен в `ALL`

- `400 Bad Request` - некорректные параметры
- `401 Unauthorized` - отсутствует или неверный сервисный токен
- `403 Forbidden` - нет прав для выполнения операции
- `500 Internal Server Error` - внутренняя ошибка сервера

---

## Структуры данных

### CreateStickerSetDto

```json
{
  "name": "string (обязательное, 1-200 символов)",
  "title": "string (опционально, макс. 64 символа)",
  "description": "string (опционально)",
  "categoryKeys": ["string"] (опционально),
  "visibility": "PUBLIC | PRIVATE" (опционально, по умолчанию PRIVATE для internal API),
  "isPublic": "boolean (устарело, используйте visibility)"
}
```

### StickerSetDto

Основной DTO для представления стикерсета. Содержит полную информацию о стикерсете, включая данные из Telegram Bot API (если `shortInfo=false`).

### PageResponse<T>

Структура пагинированного ответа:

```json
{
  "content": [T],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

## Обработка ошибок

Все ошибки возвращаются с соответствующими HTTP статусами. В случае ошибок валидации или бизнес-логики в теле ответа может быть дополнительная информация:

```json
{
  "error": "Validation error",
  "message": "Описание ошибки"
}
```

---

## Примечания

1. **Видимость стикерсетов:** По умолчанию для internal API создаются стикерсеты с видимостью `PRIVATE`. Используйте поле `visibility` в запросе создания или эндпоинты `/publish`/`/unpublish` для управления видимостью.

2. **Имя стикерсета:** Поле `name` может быть как именем стикерсета (`my_stickers_by_bot`), так и полным URL (`https://t.me/addstickers/my_stickers_by_bot`). В обоих случаях имя будет нормализовано.

3. **Межсервисные вызовы:** Все эндпоинты `/internal/*` являются доверенными и не проверяют права доступа пользователя. Они возвращают все данные, включая приватные стикерсеты.

4. **Язык:** Заголовок `X-Language` влияет на локализацию категорий и других текстовых данных. Поддерживаются значения `ru` и `en` (по умолчанию `en`).

---

## Примеры использования

### Создание и публикация стикерсета

```bash
# 1. Создать стикерсет
POST /internal/stickersets?userId=123456789
X-Service-Token: <token>
Content-Type: application/json

{
  "name": "my_stickers_by_bot",
  "title": "Мои стикеры",
  "categoryKeys": ["animals"],
  "visibility": "PRIVATE"
}

# Ответ: 201 Created с StickerSetDto (id=123)

# 2. Опубликовать стикерсет
POST /internal/stickersets/123/publish
X-Service-Token: <token>

# Ответ: 200 OK с обновленным StickerSetDto (visibility=PUBLIC)
```

### Проверка существования и получение стикерсета

```bash
# 1. Проверить наличие
GET /internal/stickersets/check?name=my_stickers_by_bot
X-Service-Token: <token>

# Ответ: {"exists": true, "name": "my_stickers_by_bot", "id": 123, "title": "Мои стикеры"}

# 2. Получить полную информацию
GET /internal/stickersets/123?shortInfo=false
X-Service-Token: <token>

# Ответ: 200 OK с полным StickerSetDto
```

### Получение стикерсетов автора с фильтрацией

```bash
GET /internal/stickersets/author/123456789?page=0&size=10&categoryKeys=animals,cute&sort=createdAt&direction=DESC
X-Service-Token: <token>
X-Language: ru

# Ответ: 200 OK с PageResponse<StickerSetDto>
```



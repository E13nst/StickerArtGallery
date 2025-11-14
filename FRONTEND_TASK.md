# Задача для фронтенда: Обновление API эндпоинтов для стикерсетов

## Описание изменений

В бэкенде были внесены изменения в API для унификации эндпоинтов. Некоторые специализированные эндпоинты были удалены, их функциональность теперь доступна через единый эндпоинт `GET /api/stickersets` с параметрами запроса.

## Удаленные эндпоинты и их замена

### 1. ❌ `GET /api/stickersets/user/{userId}`
**Заменен на:** `GET /api/stickersets?userId={userId}`

**Пример:**
```javascript
// Старый способ (больше не работает)
fetch(`/api/stickersets/user/${userId}`)

// Новый способ
fetch(`/api/stickersets?userId=${userId}`)
```

### 2. ❌ `GET /api/stickersets/author/{authorId}`
**Заменен на:** `GET /api/stickersets?authorId={authorId}`

**Пример:**
```javascript
// Старый способ (больше не работает)
fetch(`/api/stickersets/author/${authorId}`)

// Новый способ
fetch(`/api/stickersets?authorId=${authorId}`)
```

### 3. ❌ `GET /api/stickersets/top-bylikes`
**Заменен на:** `GET /api/stickersets?sort=likesCount&direction=DESC`

**Пример:**
```javascript
// Старый способ (больше не работает)
fetch('/api/stickersets/top-bylikes?page=0&size=20')

// Новый способ
fetch('/api/stickersets?sort=likesCount&direction=DESC&page=0&size=20')
```

## Новый параметр: userId

Добавлен новый параметр `userId` для фильтрации стикерсетов по пользователю:

```javascript
// Получить стикерсеты конкретного пользователя
fetch(`/api/stickersets?userId=${userId}`)

// Комбинирование с другими фильтрами
fetch(`/api/stickersets?userId=${userId}&categoryKeys=animals,cute&officialOnly=true`)
```

## Полный список параметров GET /api/stickersets

Все параметры опциональны (кроме пагинации, если нужна):

| Параметр | Тип | Описание | Пример |
|----------|-----|----------|--------|
| `page` | number | Номер страницы (начиная с 0) | `0` |
| `size` | number | Количество элементов на странице (1-100) | `20` |
| `sort` | string | Поле для сортировки | `createdAt`, `likesCount` |
| `direction` | string | Направление сортировки | `ASC`, `DESC` |
| `categoryKeys` | string | Фильтр по категориям (через запятую) | `animals,memes` |
| `officialOnly` | boolean | Только официальные стикерсеты | `true`, `false` |
| `authorId` | number | Фильтр по автору (Telegram ID) | `123456789` |
| `hasAuthorOnly` | boolean | Только стикерсеты с автором | `true`, `false` |
| **`userId`** | **number** | **Фильтр по пользователю (Telegram ID)** | **`123456789`** |
| `likedOnly` | boolean | Только лайкнутые стикерсеты | `true`, `false` |
| `shortInfo` | boolean | Без telegramStickerSetInfo | `true`, `false` |

## Что нужно сделать

1. **Найти все использования удаленных эндпоинтов:**
   - Поиск по коду: `/api/stickersets/user/`
   - Поиск по коду: `/api/stickersets/author/`
   - Поиск по коду: `/api/stickersets/top-bylikes`

2. **Заменить вызовы на новый формат:**
   - Заменить пути на query-параметры
   - Обновить функции/методы, которые формируют URL

3. **Обновить функции загрузки данных:**
   ```javascript
   // Пример функции для получения стикерсетов пользователя
   async function getUserStickerSets(userId, page = 0, size = 20) {
       const params = new URLSearchParams({
           userId: userId,
           page: page,
           size: size
       });
       const response = await fetch(`/api/stickersets?${params}`);
       return response.json();
   }
   
   // Пример функции для получения топа по лайкам
   async function getTopStickerSets(page = 0, size = 20, officialOnly = false) {
       const params = new URLSearchParams({
           sort: 'likesCount',
           direction: 'DESC',
           page: page,
           size: size,
           officialOnly: officialOnly
       });
       const response = await fetch(`/api/stickersets?${params}`);
       return response.json();
   }
   
   // Пример функции для получения стикерсетов автора
   async function getAuthorStickerSets(authorId, page = 0, size = 20) {
       const params = new URLSearchParams({
           authorId: authorId,
           page: page,
           size: size
       });
       const response = await fetch(`/api/stickersets?${params}`);
       return response.json();
   }
   ```

4. **Проверить совместимость:**
   - Убедиться, что все существующие фильтры работают корректно
   - Протестировать комбинирование параметров

## Примеры комбинирования параметров

```javascript
// Стикерсеты пользователя с фильтром по категориям
fetch(`/api/stickersets?userId=123456789&categoryKeys=animals,cute`)

// Топ официальных стикерсетов автора
fetch(`/api/stickersets?sort=likesCount&direction=DESC&officialOnly=true&authorId=123456789`)

// Лайкнутые стикерсеты пользователя
fetch(`/api/stickersets?userId=123456789&likedOnly=true`)
```

## Важные замечания

- ✅ Все старые эндпоинты **удалены** и больше не работают
- ✅ Новый эндпоинт поддерживает все те же фильтры, что и старые
- ✅ Формат ответа не изменился (тот же `PageResponse<StickerSetDto>`)
- ✅ Все параметры опциональны, можно использовать только нужные

## Тестирование

После обновления необходимо протестировать:
1. Загрузку стикерсетов пользователя
2. Загрузку стикерсетов автора
3. Загрузку топа по лайкам
4. Комбинирование фильтров
5. Пагинацию во всех случаях




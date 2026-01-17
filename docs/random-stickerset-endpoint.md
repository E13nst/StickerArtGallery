# Эндпоинт для получения случайного стикерсета

## Описание
Новый эндпоинт `GET /api/stickersets/random` возвращает случайный публичный и активный стикерсет, который пользователь еще не оценивал (не ставил лайк или дизлайк).

## Использование

### HTTP Request
```http
GET /api/stickersets/random?shortInfo=false
```

### Query Parameters
- `shortInfo` (optional, default: `false`) - если `true`, возвращает только базовую информацию без данных из Telegram Bot API

### Headers
- `X-Telegram-Init-Data` - данные авторизации из Telegram Web App (обязательно)
- `X-Language` (optional) - язык локализации (`ru` или `en`)

### Response Codes
- **200 OK** - случайный стикерсет успешно получен
- **401 Unauthorized** - пользователь не авторизован
- **404 Not Found** - нет доступных стикерсетов, которые пользователь еще не оценил
- **500 Internal Server Error** - внутренняя ошибка сервера

### Response Example
```json
{
  "id": 42,
  "userId": 987654321,
  "title": "Случайный стикерсет",
  "name": "random_stickers_by_StickerGalleryBot",
  "createdAt": "2025-01-10T15:30:00",
  "likesCount": 25,
  "dislikesCount": 3,
  "isLikedByCurrentUser": false,
  "isDislikedByCurrentUser": false,
  "telegramStickerSetInfo": "{\"name\":\"random_stickers_by_StickerGalleryBot\",\"title\":\"Случайный стикерсет\",\"sticker_type\":\"regular\",\"is_animated\":false,\"stickers\":[...]}",
  "categories": [
    {
      "id": 3,
      "key": "memes",
      "name": "Мемы",
      "description": "Мемные стикеры",
      "iconUrl": null,
      "displayOrder": 50,
      "isActive": true
    }
  ],
  "isPublic": true,
  "isBlocked": false,
  "blockReason": null
}
```

## Логика работы

### SQL запрос
Эндпоинт использует следующий SQL запрос для получения случайного стикерсета:

```sql
SELECT ss.* FROM stickersets ss 
WHERE ss.state = 'ACTIVE' 
AND ss.visibility = 'PUBLIC' 
AND ss.id NOT IN (
  SELECT l.stickerset_id FROM likes l WHERE l.user_id = :userId
) 
AND ss.id NOT IN (
  SELECT d.stickerset_id FROM dislikes d WHERE d.user_id = :userId
) 
ORDER BY RANDOM() 
LIMIT 1
```

### Фильтрация
Эндпоинт исключает:
1. Стикерсеты, на которые пользователь уже поставил лайк (таблица `likes`)
2. Стикерсеты, на которые пользователь уже поставил дизлайк (таблица `dislikes`)
3. Неактивные стикерсеты (`state != 'ACTIVE'`)
4. Приватные стикерсеты (`visibility != 'PUBLIC'`)

### Особенности
- Каждый вызов возвращает случайный стикерсет (используется `ORDER BY RANDOM()`)
- Если все доступные стикерсеты уже оценены, возвращается **404 Not Found**
- Поддерживает обогащение данными из Telegram Bot API (если `shortInfo=false`)
- Локализует категории в соответствии с языком пользователя

## Использование на фронтенде

### JavaScript/TypeScript Example
```typescript
async function getRandomStickerset(initData: string): Promise<StickerSet | null> {
  try {
    const response = await fetch('/api/stickersets/random?shortInfo=false', {
      method: 'GET',
      headers: {
        'X-Telegram-Init-Data': initData,
        'X-Language': 'ru'
      }
    });
    
    if (response.status === 404) {
      console.log('Все стикерсеты уже оценены!');
      return null;
    }
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Ошибка при получении случайного стикерсета:', error);
    throw error;
  }
}
```

### Пример использования в React
```tsx
function RandomStickersetButton() {
  const [loading, setLoading] = useState(false);
  const [stickerset, setStickerset] = useState<StickerSet | null>(null);
  
  const fetchRandom = async () => {
    setLoading(true);
    try {
      const initData = window.Telegram.WebApp.initData;
      const result = await getRandomStickerset(initData);
      
      if (result === null) {
        alert('Вы оценили все доступные стикерсеты!');
      } else {
        setStickerset(result);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div>
      <button onClick={fetchRandom} disabled={loading}>
        {loading ? 'Загрузка...' : 'Показать случайный стикерсет'}
      </button>
      
      {stickerset && (
        <div>
          <h3>{stickerset.title}</h3>
          <p>Лайков: {stickerset.likesCount}</p>
          <p>Дизлайков: {stickerset.dislikesCount}</p>
        </div>
      )}
    </div>
  );
}
```

## Тестирование

### cURL Example
```bash
# Замените YOUR_TELEGRAM_INIT_DATA на реальные данные из Telegram Web App
curl -X GET "http://localhost:8080/api/stickersets/random?shortInfo=false" \
  -H "X-Telegram-Init-Data: YOUR_TELEGRAM_INIT_DATA" \
  -H "X-Language: ru"
```

### Проверка в базе данных
Для проверки, сколько стикерсетов доступно для пользователя:

```sql
-- Заменить 123456789 на реальный Telegram ID пользователя
SELECT COUNT(*) as available_stickersets
FROM stickersets ss 
WHERE ss.state = 'ACTIVE' 
AND ss.visibility = 'PUBLIC' 
AND ss.id NOT IN (
  SELECT l.stickerset_id FROM likes l WHERE l.user_id = 123456789
) 
AND ss.id NOT IN (
  SELECT d.stickerset_id FROM dislikes d WHERE d.user_id = 123456789
);
```

## Изменения в коде

### Файлы
1. **StickerSetRepository.java** - добавлен метод `findRandomStickerSetNotRatedByUser`
2. **StickerSetService.java** - добавлен метод `findRandomStickerSetNotRatedByUser`
3. **StickerSetController.java** - добавлен эндпоинт `GET /api/stickersets/random`

### Методы
- `StickerSetRepository.findRandomStickerSetNotRatedByUser(Long userId)` - возвращает `Optional<StickerSet>`
- `StickerSetService.findRandomStickerSetNotRatedByUser(Long userId, String language, boolean shortInfo)` - возвращает `StickerSetDto`
- `StickerSetController.getRandomStickerSet(boolean shortInfo, HttpServletRequest request)` - возвращает `ResponseEntity<StickerSetDto>`

## Swagger/OpenAPI
Эндпоинт автоматически документирован в Swagger UI с примерами запросов и ответов. Доступ к документации:
```
http://localhost:8080/swagger-ui.html
```

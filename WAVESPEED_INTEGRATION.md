# Интеграция с WaveSpeed Endpoint-ами

> Обновление: c `2026-03` генерация в `sticker-art-gallery` должна идти через `STICKER_PROCESSOR`.
> Прямой flow в WaveSpeed считается legacy и оставлен только для обратной совместимости.

Этот документ описывает, как другому сервису интегрироваться с новыми асинхронными endpoint-ами генерации стикеров через WaveSpeed.

## API Sticker Art Gallery (v2)

Новые endpoint-ы приложения:

- `POST /api/generation/v2/generate` — запуск генерации через `STICKER_PROCESSOR`
- `GET /api/generation/v2/status/{taskId}` — статус внутренней задачи
- `GET /api/generation/v2/history` — история задач нового flow (`generation-v2`) для текущего пользователя
- `POST /api/generation/v2/save-to-set` — сохранение в Telegram set через `STICKER_PROCESSOR`

### Совместимость с пресетами и энхенсерами

Новые ручки `v2` используют текущий pipeline обработки промпта:

1. Применяются активные Prompt Enhancers пользователя.
2. Если в запросе передан `stylePresetId`, применяется legacy Style Preset.
3. В `STICKER_PROCESSOR` отправляется уже обработанный промпт.

Старые endpoint-ы:

- `POST /api/generation/generate`
- `GET /api/generation/status/{taskId}`
- `GET /api/generation/history`

помечены как `deprecated` и не рекомендуются для новых интеграций.

### ART-списание

Для v2 ART списываются только при успешном завершении генерации (`COMPLETED`).
При terminal fail/timeout списание не выполняется.

## Полная документация новых ручек (`/api/generation/v2/*`)

Базовый URL приложения (локально): `http://localhost:8080`

### Авторизация

Все ручки требуют заголовок:

- `X-Telegram-Init-Data: <initData>`

Пример:

```bash
-H "X-Telegram-Init-Data: user=...&hash=..."
```

### 1) `POST /api/generation/v2/generate`

Запускает асинхронную генерацию.  
Внутри перед отправкой в `sticker-processor` выполняется:

1. Prompt Enhancers пользователя (если есть и включены)
2. Style Preset по `stylePresetId` (если передан)
3. Отправка обработанного промпта в upstream

#### Тело запроса

Обязательные поля:

- `prompt: string`
- `model: "flux-schnell" | "nanabanana"`

Опциональные:

- `size: string` (default `512*512`)
- `seed: int` (default `-1`)
- `num_images: int` (сейчас `1`)
- `strength: float` (default `0.8`)
- `remove_background: bool` (default `false`)
- `image_id: string` (формат `img_...`, single-image)
- `image_ids: string[]` (multi-image; если передан непустой массив — он приоритетнее `image_id`)
- `stylePresetId: long` (legacy-compatible, но рабочий)

#### Пример запроса (single-image + legacy preset)

```bash
curl -X POST "http://localhost:8080/api/generation/v2/generate" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: <INIT_DATA>" \
  -d '{
    "prompt": "gold dragonfly sticker, transparent background",
    "model": "flux-schnell",
    "remove_background": true,
    "stylePresetId": 1,
    "image_id": "img_9f7ab3c2"
  }'
```

#### Успешный ответ

```json
{
  "taskId": "2fde3696-5478-4d65-a0c4-30b8203a319c"
}
```

---

### 2) `GET /api/generation/v2/status/{taskId}`

Возвращает текущее состояние задачи.

Статусы:

- `PROCESSING_PROMPT`
- `PENDING`
- `GENERATING`
- `COMPLETED`
- `FAILED`
- `TIMEOUT`

#### Пример запроса

```bash
curl -X GET "http://localhost:8080/api/generation/v2/status/<TASK_ID>" \
  -H "accept: application/json" \
  -H "X-Telegram-Init-Data: <INIT_DATA>"
```

#### Пример ответа `COMPLETED`

```json
{
  "taskId": "6a6eb6a5-7269-477a-a816-5c5d99f2657e",
  "status": "COMPLETED",
  "imageUrl": "https://stickerartgallery-e13nst.amvera.io/api/images/3ab68a46-0826-47fe-9db6-83dbd9c1e0bc.webp",
  "originalImageUrl": "https://sticker-processor-e13nst.amvera.io/stickers/wavespeed/ws_e4acfbe8c7d4609ff12bab5b",
  "errorMessage": null
}
```

---

### 3) `GET /api/generation/v2/history`

История генераций текущего пользователя только для нового flow (`generation-v2`).

Query params:

- `page` (default `0`)
- `size` (default `20`)

#### Пример запроса

```bash
curl -X GET "http://localhost:8080/api/generation/v2/history?page=0&size=20" \
  -H "accept: application/json" \
  -H "X-Telegram-Init-Data: <INIT_DATA>"
```

Ответ: пагинированный список `GenerationStatusResponse`.

---

### 4) `POST /api/generation/v2/save-to-set`

Сохраняет сгенерированный стикер в Telegram set через `sticker-processor`.

#### Тело запроса

- `taskId: string` (ID задачи gallery)
- `userId: long` (Telegram user ID)
- `name: string` (short name)
- `title: string`
- `emoji: string` (default `😀`)
- `wait_timeout_sec: int` (default `60`)

#### Пример запроса

```bash
curl -X POST "http://localhost:8080/api/generation/v2/save-to-set" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Init-Data: <INIT_DATA>" \
  -d '{
    "taskId": "6a6eb6a5-7269-477a-a816-5c5d99f2657e",
    "userId": 141614461,
    "name": "my_pack_by_bot",
    "title": "My Pack",
    "emoji": "😀",
    "wait_timeout_sec": 60
  }'
```

#### Ответ

- `200` — сохранено
- `202` — не готово в пределах wait timeout
- `400/404/410/422/424` — terminal ошибки upstream

## Рекомендуемый client flow (v2)

1. `POST /api/generation/v2/generate` -> получить `taskId`
2. Poll `GET /api/generation/v2/status/{taskId}` до terminal статуса
3. При `COMPLETED` использовать:
  - `imageUrl` для фронта/превью
  - `originalImageUrl` как upstream источник
4. Опционально вызвать `POST /api/generation/v2/save-to-set`

Примечание по `remove_background`:

- если upstream не смог удалить фон, `sticker-art-gallery` автоматически делает один fallback-повтор без удаления фона;
- если fallback успешен, задача всё равно завершится `COMPLETED`, а детали fallback будут в `metadata`.

## Ошибки и диагностика

- Если задача падает сразу с `FAILED` и `400 BAD_REQUEST` от sticker-processor, проверьте корректность `image_id/image_ids`.
- Если задача падает с `404`, проверьте TTL загруженного изображения и корректность `img_...` идентификатора.
- Для анализа пайплайна используйте админ-логи:
  - `/api/admin/generation-logs` (audit timeline)
  - `/api/admin/generation-v2` (история новых задач)

## Endpoint-ы

- `POST /stickers/wavespeed/generate` - отправка задачи генерации, получение синтетического `ws_...` file id
- `GET /stickers/wavespeed/{file_id}` - получение готового стикера (`image/webp`) или текущего статуса задачи
- `POST /stickers/wavespeed/save-to-set` - дождаться готовности `ws_...` и сохранить в Telegram sticker set

Пример локового base URL: `http://127.0.0.1:8081`

## Рекомендуемый flow интеграции

1. Отправить задачу через `POST /stickers/wavespeed/generate`
2. Сохранить возвращённый `file_id` (`ws_...`) в своей системе
3. Выполнять polling `GET /stickers/wavespeed/{file_id}` с retry/backoff
4. Если ответ `200`, сохранить/передать байты WebP
5. Если ответ `202`, продолжать polling
6. Если ответ terminal `4xx/5xx` (`400`, `404`, `410`, `422`, `424`) - завершить задачу как failed
7. (Опционально) Вызвать `POST /stickers/wavespeed/save-to-set` для автоматического добавления в стикерсет

## Модель запроса: `POST /stickers/wavespeed/generate`

### Обязательные поля

- `prompt: string`
- `model: "flux-schnell" | "nanabanana"`

### Опциональные поля

- `size: string` (по умолчанию: `"512*512"`)
- `seed: int` (по умолчанию: `-1`)
- `num_images: int` (сейчас должен быть `1`)
- `strength: float` (по умолчанию: `0.8`)
- `remove_background: bool` (по умолчанию: `false`)
- `source_image_ids: string[]` (опционально для text2img, обязательно для image-edit; элементы в формате `img_...`)

### Выбор режима Nano Banana

Для `model="nanabanana"` режим выбирается автоматически:

- если передан непустой `source_image_ids` -> режим image edit
- если `source_image_ids` не передан -> режим text-to-image

## Модель ответа: `POST /stickers/wavespeed/generate`

Успех (`202 Accepted`):

```json
{
  "file_id": "ws_1e6a2979a4d45754c16f9e97",
  "status": "pending",
  "provider_request_id": "288f54cbd97747d0a9ed993ced8b6a9f"
}
```

Ошибка валидации/провайдера (`400`):

```json
{
  "detail": "..."
}
```

Внутренняя ошибка (`500`):

```json
{
  "detail": "Failed to submit WaveSpeed generation: ..."
}
```

## Поведение ответа: `GET /stickers/wavespeed/{file_id}`

- `200 OK` -> тело ответа это бинарный `image/webp` (совместим с Telegram sticker)
- `202 Accepted` -> задача ещё обрабатывается:
  ```json
  {"file_id":"ws_...","status":"pending"}
  ```
- `400 Bad Request` -> некорректный формат `file_id` (должен начинаться с `ws_`)
- `404 Not Found` -> задача не найдена
- `410 Gone` -> задача просрочена (TTL истёк)
- `422 Unprocessable Entity` -> семантическая ошибка обработки
- `424 Failed Dependency` -> ошибка upstream/post-processing (generation/download/background removal)

Для failed-задач `detail` обычно содержит:

```json
{
  "detail": {
    "code": "generation_failed|download_failed|background_removal_failed|...",
    "message": "человекочитаемая причина"
  }
}
```

## Автосохранение в стикерсет: `POST /stickers/wavespeed/save-to-set`

Endpoint принимает `ws_` `file_id`, ждёт готовности стикера (до `wait_timeout_sec`) и затем:

- если стикерсет уже существует -> добавляет стикер;
- если стикерсета нет -> создаёт новый набор (`name` + `title`) и добавляет первый стикер.
- при необходимости автоматически нормализует `name`: добавляет суффикс `_by_<TELEGRAM_BOT_USERNAME>`.

Поля запроса:

- `file_id: string` (обязательно, `ws_...`)
- `user_id: int` (обязательно, владелец стикерсета в Telegram)
- `name: string` (обязательно, short name стикерсета)
- `title: string` (обязательно, title для создания набора)
- `emoji: string` (опционально, emoji, который привязывается к стикеру; по умолчанию `😀`)
- `wait_timeout_sec: int` (опционально, по умолчанию `60`)

Ответы:

- `200` - стикер успешно сохранён/добавлен в набор
- `202` - генерация ещё не готова в пределах `wait_timeout_sec`
- `404` - `ws_` job не найден
- `410` - `ws_` job истёк
- `422` - неподдерживаемый формат для сохранения (ожидается static `image/webp`)
- `424` - ошибка генерации/post-processing перед сохранением

## Стратегия polling для production

Используйте ограниченные retries с exponential backoff и jitter.

Рекомендуемые значения:

- начальная задержка: `1s`
- множитель: `1.5` или `2.0`
- максимальная задержка: `10s`
- общий timeout budget: `60-120s`

Псевдо-flow:

```text
submit -> получить ws_file_id
loop до дедлайна:
  GET /stickers/wavespeed/{file_id}
  if 200: done
  if 202: wait(backoff+jitter), continue
  if 400/404/410/422/424: terminal fail
  else: retry по вашей platform policy
```

## Примеры cURL

### 1) flux-schnell text-to-image

```bash
curl -X POST "http://127.0.0.1:8081/stickers/wavespeed/generate" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "gold dragonfly sticker, transparent background",
    "model": "flux-schnell",
    "size": "512*512",
    "seed": -1,
    "num_images": 1,
    "strength": 0.8,
    "remove_background": true
  }'
```

### 2) nanabanana text-to-image

```bash
curl -X POST "http://127.0.0.1:8081/stickers/wavespeed/generate" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "fat gold cat with rick and morty style",
    "model": "nanabanana",
    "remove_background": false
  }'
```

### 3) nanabanana image edit (source image ids)

```bash
curl -X POST "http://127.0.0.1:8081/stickers/wavespeed/generate" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Turn this image into telegram sticker style",
    "model": "nanabanana",
    "source_image_ids": ["img_e7d0aa12"],
    "remove_background": true
  }'
```

### 4) polling/скачивание готового стикера

```bash
# Замените ws_xxx на file_id из ответа POST
curl -v "http://127.0.0.1:8081/stickers/wavespeed/ws_xxx" --output sticker.webp
```

### 5) сохранить готовый `ws_` стикер в Telegram set

```bash
curl -X POST "http://127.0.0.1:8081/stickers/wavespeed/save-to-set" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "file_id": "ws_xxx",
    "user_id": 123456789,
    "name": "my_pack_by_your_bot",
    "title": "My Pack",
    "emoji": "😀",
    "wait_timeout_sec": 60
  }'
```

Примечания:

- Если ответ `202`, файл изображения ещё не готов.
- Если ответ `200`, `sticker.webp` готов к использованию в Telegram sticker flow.

## Важные примечания для интеграции

- `file_id` синтетический и namespaced (`ws_...`), это не Telegram `file_id`.
- Финальный результат нормализуется в Telegram-compatible WebP (canvas 512x512 с сохранением пропорций).
- Сгенерированные файлы кешируются; повторные `GET` для готовых задач быстрые.
- В production-клиентах передавайте только реальные `img_...` ID в `image_id/image_ids` (или `source_image_ids` для прямой интеграции).
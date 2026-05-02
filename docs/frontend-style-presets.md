# Style Presets для фронтенда

## Что это такое

На бэкенде "стиль" для генерации стикера сейчас хранится как `style preset`.
Это не отдельный файл с полным prompt template, а в первую очередь текстовый `promptSuffix`, который дописывается к пользовательскому промпту перед генерацией.

Pipeline такой:

1. Пользователь вводит `prompt`.
2. Бэкенд прогоняет его через `prompt_enhancers` (например перевод на английский и раскрытие эмоций).
3. Если передан `stylePresetId`, бэкенд находит пресет и дописывает его `promptSuffix`.
4. Уже итоговый prompt уходит в генерацию.

## Где это хранится

Основной источник истины:

- `src/main/resources/db/migration/V1_0_38__Create_presets_and_enhancers.sql`

Там создаются таблицы:

- `style_presets`
- `prompt_enhancers`

И там же сидятся стартовые глобальные пресеты:

- `telegram_sticker`
- `anime`
- `simpsons`

## Где редактируются и создаются

### Через API

Контроллер:

- `src/main/java/com/example/sticker_art_gallery/controller/StylePresetController.java`

Основные эндпоинты:

- `GET /api/generation/style-preset-categories` - список категорий (порядок: `sortOrder`, затем `name`)
- `GET /api/generation/style-presets` - доступные пользователю пресеты
- `GET /api/generation/style-presets/my` - персональные пресеты пользователя
- `GET /api/generation/style-presets/global` - все глобальные пресеты, только admin
- `POST /api/generation/style-presets` - создать персональный пресет
- `POST /api/generation/style-presets/global` - создать глобальный пресет, только admin
- `PUT /api/generation/style-presets/{id}` - обновить пресет
- `PUT /api/generation/style-presets/{id}/toggle?enabled=true|false` - включить/выключить пресет, только admin
- `DELETE /api/generation/style-presets/{id}` - удалить пресет

### Через админку

Текущая админская страница:

- `src/main/resources/static/admin/style-presets.html`
- `src/main/resources/static/admin/js/style-presets.js`

Она работает через тот же API.

### Через миграции/БД

Если нужны дефолтные пресеты для всех окружений, правильно добавлять их новой Flyway-миграцией, а не руками в базе.

## Какие поля есть у пресета

В запросе на создание:

- `code` - уникальный код пресета, например `anime`
- `name` - отображаемое имя
- `description` - короткое описание
- `promptSuffix` - текст, который дописывается к prompt
- `removeBackground` - политика удаления фона: `true`, `false` или `null` для fallback к значению запроса
- `sortOrder` - порядок **внутри категории**
- `categoryId` (в запросе создания/обновления) - ID категории; если не задан, подставляется категория `general`

В ответе бэкенд отдаёт:

- `id`
- `code`
- `name`
- `description`
- `promptSuffix`
- `removeBackground`
- `isGlobal`
- `ownerId`
- `isEnabled`
- `sortOrder` (внутри категории)
- `category` — `{ id, code, name, sortOrder }`
- `createdAt`
- `updatedAt`

Файлы:

- `src/main/java/com/example/sticker_art_gallery/dto/generation/CreateStylePresetRequest.java`
- `src/main/java/com/example/sticker_art_gallery/dto/generation/StylePresetDto.java`

## Как подключать на фронте

Для списка пресетов лучше использовать:

- `GET /api/generation/style-presets`

Почему именно так:

- вернутся только доступные пользователю пресеты
- уже придут и глобальные, и персональные
- отключенные пресеты туда не должны попадать

Для генерации нужно передавать:

- `stylePresetId`
- `removeBackground` / `remove_background` только как fallback, если выбранный пресет не задает свою политику

Это поле есть в:

- `src/main/java/com/example/sticker_art_gallery/dto/generation/GenerateStickerV2Request.java`

Практически для фронта схема такая:

1. Загружаешь список пресетов.
2. Показываешь карточки по `id`, `code`, `name`, `description`.
3. При выборе карточки сохраняешь `stylePresetId`.
4. При генерации отправляешь выбранный `stylePresetId` вместе с `prompt`.
5. Если пресет выбран и у него есть `removeBackground`, backend использует его.
6. Если пресет не выбран или у пресета `removeBackground = null`, backend использует значение кнопки `RemoveBackground`.

Важно: для генерации сейчас нужен именно `id`, не `code`.
Поэтому на фронте лучше не хардкодить айдишники, а всегда сначала получать список с бэка и уже потом искать нужный пресет по `code`.

## Как добавить больше пресетов для ленты вдохновения

Для новых вариантов вроде `poster`, `night_drawing`, `anime_soft`, `studio_portrait`, `comic_book` сейчас есть два нормальных пути:

1. Добавлять глобальные пресеты через admin API.
2. Если это базовый набор продукта, добавить их новой миграцией как глобальные дефолты.

Минимальный payload:

```json
{
  "code": "studio_portrait",
  "name": "Studio Portrait",
  "description": "Студийный портрет, чистый свет, аккуратная детализация",
  "promptSuffix": ", studio portrait style, soft controlled lighting, clean composition, high detail",
  "sortOrder": 10
}
```

## Где именно лежат промты

Если речь про стили из ленты вдохновения, то их промты лежат не в `ai/prompts`, а в поле:

- `style_presets.prompt_suffix`

Если речь про AI-предобработку prompt, то это уже другая сущность:

- `prompt_enhancers.system_prompt`

То есть:

- стиль = `promptSuffix`
- предобработка текста = `systemPrompt` у enhancer

## Что важно учесть на фронте

### 1. У пресета сейчас нет нормального поля превью

В DTO нет `thumbnail`, `image`, `previewUrl` или `icon`.
Для ленты вдохновения это значит, что одних данных бэка недостаточно для красивой визуальной витрины.

Сейчас есть 2 варианта:

1. Держать превью-картинки и декоративные метаданные на фронте в маппинге по `code`.
2. Доработать бэкенд и добавить в `style_presets` поле вроде `preview_image_url`.

### 2. `promptSuffix` это просто хвост

Сейчас пресет не заменяет пользовательский промпт полностью и не задаёт сложную структуру.
Он просто дописывается в конец.
Поэтому названия и описания на фронте должны помогать пользователю понять, какой результат даёт этот хвост.

### 3. Для стабильности лучше опираться на `code`

В UI удобно жить по `code`, а перед генерацией брать реальный `id` из ответа API.
Так фронт не сломается, если на другом окружении айдишники будут другими.

## Рекомендация для твоей задачи

Если ты хочешь сделать большую "ленту идей для вдохновения", то правильная схема сейчас такая:

1. Бэкенд хранит список пресетов и их `promptSuffix`.
2. Фронт запрашивает пресеты с API.
3. Фронт добавляет свою витринную мета-информацию по `code`:
  - обложка
  - локализованное название
  - короткий маркетинговый текст
  - группа (`portrait`, `anime`, `poster`, `night`, `fun`)
4. При выборе карточки фронт передаёт в генерацию `stylePresetId`.

## Короткий вывод

Сейчас существующие стили `anime`, `simpsons`, `telegram_sticker` хранятся в таблице `style_presets`.
Редактируются через `StylePresetController` и админку `style-presets.html`.
Главный "промт стиля" - это `promptSuffix`.

Для фронта лучший путь:

- брать список через `GET /api/generation/style-presets`
- отображать по `code`
- генерировать по `stylePresetId`
- превью и оформление ленты пока хранить отдельно от бэкенда

## Публичные ссылки (deep link в мини-приложение)

Бэкенд в `StylePresetDto` отдаёт:

- `shareableAsDeepLink` — `true`, если стиль можно предлагать чужим людям (глобальный активный пресет **или** пользовательский с `moderationStatus=APPROVED` и `publishedToCatalog=true`, и пресет включён).
- `deepLinkStartParam` — строка для параметра Mini App, **без** обёртки URL; `null`, если шаринг недоступен.

Формат значения фиксирован: префикс `sag_style_` + **числовой id** пресета (см. `StylePresetDeepLinkParams` в бэкенде). Так избегают коллизий по `code` у разных владельцев. Реферальные ссылки используют префикс `ref_` — не пересекается.

### Задачи фронтенда мини-приложения

1. Кнопка «Поделиться» / копирование ссылки только если `shareableAsDeepLink === true` и есть `deepLinkStartParam`.
2. Собрать URL для Telegram: `https://t.me/<bot_username>/<mini_app_short_name>?startapp=<deepLinkStartParam>` (или актуальный для проекта формат Direct Link / меню бота).
3. При старте прочитать `Telegram.WebApp.initDataUnsafe.start_param` (или разбор `initData`). Если значение парсится как `sag_style_<id>`:
  - после `GET /api/generation/style-presets?includeUi=true` найти пресет с этим `id`;
  - выбрать его для генерации;
  - в UI поднять карточку **наверх списка** (локальная сортировка поверх порядка с API), если пресет найден.
4. Если id не найден в доступном списке — мягко игнорировать (сообщение опционально).
5. Не интерпретировать `start_param` с префиксом `ref_` как пресет.


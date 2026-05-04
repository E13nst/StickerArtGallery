---
name: generate-sticker-set-endpoint
description: Добавляет новый endpoint в StickerSetController или соседний sticker-set controller по текущим CRUD и query паттернам проекта. Use when the user asks to extend sticker set API, add a CRUD operation, or create a new ResponseEntity-based sticker-set endpoint.
---

# Generate Sticker Set Endpoint

## Когда использовать

Используй этот skill, когда нужно добавить новый endpoint для операций с наборами стикеров.

## Сначала прочитай

1. `PROJECT_CONTEXT.md`
2. `src/main/java/com/example/sticker_art_gallery/controller/StickerSetController.java`
3. `src/main/java/com/example/sticker_art_gallery/controller/StickerSetQueryController.java`
4. `src/main/java/com/example/sticker_art_gallery/service/telegram/StickerSetService.java`

## Workflow

1. Сначала проверь, в какой controller должен попасть endpoint: `StickerSetController` для основного CRUD и bot-backed операций или `StickerSetQueryController` для query/read-сценариев.
2. Найди ближайший существующий метод с похожим HTTP методом и моделью ответа.
3. Переиспользуй инъекцию `StickerSetService` и другие уже внедренные зависимости `StickerSetController`. Не создавай второй параллельный service без необходимости.
4. Создай request DTO и `@Valid`-валидацию, если endpoint принимает JSON body.
5. Возвращай `ResponseEntity` и HTTP status в том же стиле, что и соседние методы: `ok`, `status(CREATED)`, `badRequest`, `notFound`, `internalServerError`.
6. Если endpoint требует авторизацию или owner/admin checks, используй текущие helper-паттерны и уже существующие проверки доступа.
7. Если endpoint трогает Telegram Bot API, делегируй вызов через `TelegramBotApiService` или `StickerSetCreationService`, а не изобретай новый клиент.
8. Если endpoint влияет на кеш или видимость набора, проверь, нужен ли вызов `StickerSetTelegramCacheService`, `StickerSetVisibilityController` или соседнего visibility flow.
9. Добавь тест, ориентируясь на `ImprovedStickerSetControllerIntegrationTest` или другой ближайший тест для sticker-set API.

## Обязательные правила

- Не дублируй уже существующий endpoint под новым URL.
- Не перемещай query-логику в write-endpoint без причины.
- Не обходи `StickerSetService`, если логика уже там живет.
- Держи ответ стандартизированным и согласованным с соседними методами.

## Проверка перед завершением

- Endpoint добавлен в правильный controller.
- Используется существующая инъекция `StickerSetService`.
- Валидация и статусы ответа согласованы с текущим API.
- Добавлен тест на основной сценарий и минимум одну ошибку.

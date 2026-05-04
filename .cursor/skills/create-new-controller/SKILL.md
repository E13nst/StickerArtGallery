---
name: create-new-controller
description: Создает новый REST controller, связанный service, request/response DTO и тесты по паттернам StickerArtGallery. Use when the user asks to add a new API endpoint, a new controller, or a new REST module in the Java backend.
---

# Create New Controller

## Когда использовать

Используй этот skill, когда нужно добавить новый REST API модуль в Java backend: новый controller, service, DTO и тесты.

## Сначала прочитай

1. `PROJECT_CONTEXT.md`
2. `src/main/java/com/example/sticker_art_gallery/controller/`
3. Ближайший по смыслу controller из раздела `1.2 REST API Контроллеры`
4. Один существующий integration test из `src/test/java/com/example/sticker_art_gallery/controller/`

## Workflow

1. Определи группу контроллера по карте из `PROJECT_CONTEXT.md`: профиль, платежи, telegram, sticker catalog, admin или другое.
2. Выбери пакет и имя по текущему соглашению проекта: `controller/*Controller.java`, `service/.../*Service.java`, `dto/.../*Request|*Response|*Dto.java`.
3. Создай controller с `@RestController` и `@RequestMapping`. Инжектируй зависимости только через конструктор.
4. Держи controller тонким: прими запрос, провалидируй `@Valid`, вызови service, верни `ResponseEntity`.
5. Вынеси бизнес-логику в service. Не помещай правила домена в controller или DTO.
6. Создай отдельные DTO для входа и выхода, если endpoint не совпадает с уже существующей формой ответа.
7. Следуй текущему стилю OpenAPI-аннотаций, только если соседние методы в выбранном controller уже используют `@Operation`, `@ApiResponses` и примеры.
8. Добавь тесты: минимум service test или controller integration test, ориентируясь на ближайший существующий шаблон.
9. Проверь, не существует ли уже похожий endpoint в соседнем controller, чтобы не дублировать API.

## Обязательные правила

- Используй `@Valid` для request DTO в `@RequestBody`.
- Не используй field injection.
- Не обращайся к repository прямо из controller.
- Возвращай стандартизированный `ResponseEntity` и HTTP status, согласованный с соседними endpoint'ами.
- Держи названия DTO в формате `*Request`, `*Response`, `*Dto`.

## Проверка перед завершением

- Controller лежит в правильной группе из `PROJECT_CONTEXT.md`.
- Service содержит бизнес-логику.
- DTO и валидация добавлены.
- Тесты покрывают успешный и минимум один неуспешный сценарий.

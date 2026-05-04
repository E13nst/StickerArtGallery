---
name: add-telegram-command-handler
description: Добавляет новую Telegram-команду или callback handler в webhook-архитектуру StickerArtGallery. Use when the user asks to add a bot command, callback_data route, or update handling logic in UpdateRouterService or StickerGalleryFlowService.
---

# Add Telegram Command Handler

## Когда использовать

Используй этот skill, когда нужно добавить новую команду, callback или обработку входящего Telegram update.

## Сначала прочитай

1. `PROJECT_CONTEXT.md`
2. `src/main/java/com/example/sticker_art_gallery/service/telegram/UpdateRouterService.java`
3. `src/main/java/com/example/sticker_art_gallery/service/telegram/chat/StickerGalleryFlowService.java`
4. Если задача про support, дополнительно прочитай `service/telegram/support/SupportBridgeService.java`

## Workflow

1. Определи тип события: команда из `message.text`, `callback_query`, `inline_query`, `web_app_query` или payment update.
2. Добавь маршрут в `UpdateRouterService` в существующую ветку, не ломая текущий порядок роутинга и early return.
3. Если это пользовательский chat flow, реализуй логику в `StickerGalleryFlowService`.
4. Если логика не помещается в текущий flow-service, создай отдельный service и вызови его из router или `StickerGalleryFlowService`, а не из controller.
5. Для callback обработчиков придерживайся текущего паттерна: распознай `callback_data`, выполни действие, верни `true`, если событие обработано.
6. Для ответа пользователю используй `TelegramBotApiService`. Не отправляй raw HTTP запросы к Telegram напрямую.
7. Если добавляется новая команда, обнови help/start тексты только если это действительно улучшает UX и не ломает текущий сценарий.
8. Если есть состояние диалога, проверь, не нужно ли интегрироваться с `SupportStateStore` или отдельным state-хранилищем.
9. Добавь тест на новый маршрут или handler в зоне `src/test/java/com/example/sticker_art_gallery/service/telegram/`.

## Обязательные правила

- Не добавляй `TelegramLongPollingBot`.
- Не обходи цепочку `TelegramUpdatesController -> UpdateRouterService`.
- Не смешивай support flow и sticker flow в одном методе без необходимости.
- После обработки callback отвечай через `answerCallbackQuery`, если это соответствует текущему паттерну.

## Проверка перед завершением

- Новый route достижим из `UpdateRouterService`.
- Логика лежит в правильном flow/service.
- Ответ пользователю идет через `TelegramBotApiService`.
- Существующие `/start`, `/help`, `/cancel`, `/support` сценарии не сломаны.
